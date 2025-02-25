/*
* Copyright 2018 Nextworks s.r.l.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package it.nextworks.nfvmano.configmanager;

import com.telcaria.kibana.dashboards.model.KibanaDashboardDescription;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.impl.HttpStatusException;
import it.nextworks.nfvmano.configmanager.alerts.AlertRepo;
import it.nextworks.nfvmano.configmanager.alerts.AlertsController;
import it.nextworks.nfvmano.configmanager.alerts.MemoryAlertRepo;
import it.nextworks.nfvmano.configmanager.common.ErrorResponse;
import it.nextworks.nfvmano.configmanager.dashboards.DashboardController;
import it.nextworks.nfvmano.configmanager.dashboards.DashboardRepo;
import it.nextworks.nfvmano.configmanager.dashboards.MemoryDashboardRepo;
import it.nextworks.nfvmano.configmanager.dashboards.kibana.KibanaDashboardController;
import it.nextworks.nfvmano.configmanager.dashboards.kibana.KibanaDashboardRepo;
import it.nextworks.nfvmano.configmanager.dashboards.kibana.MemoryKibanaDashboardRepo;
import it.nextworks.nfvmano.configmanager.dashboards.model.Dashboard;
import it.nextworks.nfvmano.configmanager.dashboards.model.DashboardDescription;
import it.nextworks.nfvmano.configmanager.exporters.ExporterController;
import it.nextworks.nfvmano.configmanager.exporters.ExporterRepo;
import it.nextworks.nfvmano.configmanager.exporters.MemoryExporterRepo;
import it.nextworks.nfvmano.configmanager.exporters.model.Exporter;
import it.nextworks.nfvmano.configmanager.exporters.model.ExporterDescription;
import it.nextworks.nfvmano.configmanager.prometheusMQAgent.PrometheusMQAgent;
import it.nextworks.nfvmano.configmanager.prometheusScraper.MemoryPrometheusScraperRepo;
import it.nextworks.nfvmano.configmanager.prometheusScraper.PrometheusScraperController;
import it.nextworks.nfvmano.configmanager.prometheusScraper.PrometheusScraperRepo;
import it.nextworks.nfvmano.configmanager.prometheusScraper.model.PrometheusScraper;
import it.nextworks.nfvmano.configmanager.rvmagent.MemoryRVMAgentRepo;
import it.nextworks.nfvmano.configmanager.rvmagent.MongoDBRVMAgentRepo;
import it.nextworks.nfvmano.configmanager.rvmagent.RVMAgentController;
import it.nextworks.nfvmano.configmanager.rvmagent.RVMAgentRepo;
import it.nextworks.nfvmano.configmanager.sb.PrometheusScraper.PrometheusScraperConnector;
import it.nextworks.nfvmano.configmanager.sb.PrometheusScraper.PrometheusScraperService;
import it.nextworks.nfvmano.configmanager.sb.grafana.GrafanaConnector;
import it.nextworks.nfvmano.configmanager.sb.grafana.GrafanaDashboardService;
import it.nextworks.nfvmano.configmanager.sb.kibana.KibanaConnector;
import it.nextworks.nfvmano.configmanager.sb.kibana.KibanaDashboardService;
import it.nextworks.nfvmano.configmanager.sb.logstash.LogstashConnector;
import it.nextworks.nfvmano.configmanager.sb.logstash.TopicService;
import it.nextworks.nfvmano.configmanager.sb.prometheus.AlertService;
import it.nextworks.nfvmano.configmanager.sb.kafkaRVMAgent.RVMAgentConnector;
import it.nextworks.nfvmano.configmanager.sb.kafkaRVMAgent.RVMAgentService;
import it.nextworks.nfvmano.configmanager.sb.prometheus.AlertService;
import it.nextworks.nfvmano.configmanager.sb.prometheus.ExporterService;
import it.nextworks.nfvmano.configmanager.sb.prometheus.MemoryTargetRepo;
import it.nextworks.nfvmano.configmanager.sb.prometheus.PrometheusConnector;
import it.nextworks.nfvmano.configmanager.sb.prometheus.TargetRepo;
import it.nextworks.nfvmano.configmanager.sb.prometheus.TargetRepo;
import it.nextworks.nfvmano.configmanager.topics.MemoryTopicRepo;
import it.nextworks.nfvmano.configmanager.topics.TopicController;
import it.nextworks.nfvmano.configmanager.topics.TopicRepo;
import it.nextworks.nfvmano.configmanager.topics.model.Topic;
import it.nextworks.nfvmano.configmanager.utils.ConfigReader;
import it.nextworks.nfvmano.configmanager.utils.Paths;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static it.nextworks.nfvmano.configmanager.utils.ContextUtils.makeParsingHandler;
import static it.nextworks.nfvmano.configmanager.utils.ContextUtils.respond;

public class MainVerticle extends AbstractVerticle {

    // STATICS

    private static final Logger log = LoggerFactory.getLogger(MainVerticle.class);

    private static final Set<String> AVAILABLE_TYPES;

    static {
        HashSet<String> temp = new HashSet<>();
        temp.add("application/json");
        temp.add("application/x-yaml");
        AVAILABLE_TYPES = Collections.unmodifiableSet(
                temp
        );
    }



    private static Route producing(Route route) {
        for (String type : AVAILABLE_TYPES) {
            route.produces(type);
        }
        return route;
    }

    // Instance members
    private int port;

    private String promConfigPath;
    private String promAlertRulesPath;
    private String promAlertManagerPath;
    private String promCalculateRulesPath;

    private String logstashConfigPath;

    private String grafanaHost;
    private int grafanaPort;
    private String grafanaToken;
    private boolean reportFullUrl;

    private ExporterController exporterController;
    private DashboardController dashboardController;
    private AlertsController alertsController;
    private RVMAgentController rvmAgentController;
    private PrometheusScraperController prometheusScraperController;

    private String promHost;
    private int promPort;
    private String amHost;
    private int amPort;
    private TopicController topicController;
    private KibanaDashboardController kibanaDashboardController;


    private PrometheusConnector promConnector;
    private RVMAgentConnector rvmAgentConnector;
    private PrometheusScraperConnector prometheusScraperConnector;
    private ConfigReader config;

    private String kafka_bootstrap_servers;
    private ConcurrentHashMap<String, HashMap<String, String>> concurrentHashMap = new ConcurrentHashMap<String, HashMap<String, String>>();;
    private String kibanaHost;
    private int kibanaPort;
    private LogstashConnector logstashConnector;

    private void readConfig() {
        this.config = new ConfigReader();

        port = config.getIntProperty("server.port");

        promConfigPath = config.getProperty("prometheus.config");
        promAlertRulesPath = config.getProperty("prometheus.alertRules");
        promCalculateRulesPath = config.getProperty("prometheus.calculateRules");
        promAlertManagerPath = config.getProperty("prometheus.alertManager");
        logstashConfigPath = config.getProperty("logstash.config");

        grafanaHost = config.getProperty("grafana.host");
        grafanaPort = config.getIntProperty("grafana.port");
        grafanaToken = config.getProperty("grafana.token");
        reportFullUrl = config.getBoolProperty("dashboards.returnFullUrl");

        promHost = config.getProperty("prometheus.host");
        promPort = config.getIntProperty("prometheus.port");
        amHost = config.getProperty("alertmanager.host");
        amPort = config.getIntProperty("alertmanager.port");

        kafka_bootstrap_servers = this.config.getProperty("kafka.bootstrap.servers");

        kibanaHost = config.getProperty("kibana.host");
        kibanaPort = config.getIntProperty("kibana.port");

        String logLevel = config.getProperty("logging.level");

        if (logLevel != null) {
            org.apache.log4j.Logger logger = LogManager.getLogger("it.nextworks.nfvmano");
            Level level = Level.toLevel(logLevel);
            logger.setLevel(level);
        }
    }

    private PrometheusConnector makePrometheusConnector() {
        WebClient webClient = WebClient.create(vertx);
        if (promConnector == null) {
            promConnector = new PrometheusConnector(
                    promConfigPath,
                    promAlertRulesPath,
                    promAlertManagerPath,
                    promCalculateRulesPath,
                    promHost,
                    promPort,
                    amHost,
                    amPort,
                    webClient
            );
        }
        return promConnector;
    }

    private RVMAgentConnector makeRVMAgentConnector() {
        if (rvmAgentConnector == null) {
            rvmAgentConnector = new RVMAgentConnector(kafka_bootstrap_servers
            );
        }
        return rvmAgentConnector;
    }

    private PrometheusScraperConnector makePrometheusScraperConnector() {
        if (prometheusScraperConnector == null) {
            prometheusScraperConnector = new PrometheusScraperConnector (kafka_bootstrap_servers);
        }
        return prometheusScraperConnector;
    }

    private LogstashConnector makeLogstashConnector() {
        if(logstashConnector == null) {
            logstashConnector = new LogstashConnector(
                    logstashConfigPath
            );
        }
        return logstashConnector;
    }


    private void makeExporterController() {
        PrometheusConnector pConnector = makePrometheusConnector();
        ExporterRepo exporterRepo = new MemoryExporterRepo(); // TODO make persistent expRepo?
        ExporterService exporterService = new ExporterService(pConnector, exporterRepo);
        exporterController = new ExporterController(exporterService);
    }

    private void makeDashboardController() {
        DashboardRepo dashboardRepo = new MemoryDashboardRepo();  // TODO make persistent dash repo?
        WebClient webClient = WebClient.create(
                vertx,
                new WebClientOptions()
                        .setDefaultHost(grafanaHost)
                        .setDefaultPort(grafanaPort)
        );
        String grafanaBaseUrl = "http://" + grafanaHost + ":" + grafanaPort;
        GrafanaConnector gConnector = new GrafanaConnector(webClient, grafanaToken, grafanaBaseUrl, reportFullUrl);
        GrafanaDashboardService service = new GrafanaDashboardService(gConnector, dashboardRepo);
        dashboardController = new DashboardController(service);
    }

    private void makeRVMAgentController() {
        RVMAgentConnector rvmConnector = makeRVMAgentConnector();
        RVMAgentRepo rvmAgentRepo  = new MemoryRVMAgentRepo();
        RVMAgentService rvmAgentService = new RVMAgentService(rvmConnector, rvmAgentRepo);
        rvmAgentController= new RVMAgentController(rvmAgentService);
    }

    private void makePrometheusScraperController() {
        PrometheusConnector pConnector = makePrometheusConnector();
        Properties configProducer = new Properties();
        configProducer.put("bootstrap.servers", kafka_bootstrap_servers);
        configProducer.put("acks", "all");
        configProducer.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        configProducer.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        configProducer.put("client.id", "ClientForScreper");
        KafkaProducer kafkaProducerForScraper = new KafkaProducer<String, String>(configProducer);
        WebClient webClient = WebClient.create(vertx);
        PrometheusScraperConnector prometheusScraperConnector = makePrometheusScraperConnector();
        PrometheusScraperRepo prometheusScraperRepo  = new MemoryPrometheusScraperRepo();
        PrometheusScraperService PrometheusScraperService = new PrometheusScraperService(prometheusScraperConnector,
                prometheusScraperRepo, concurrentHashMap, kafkaProducerForScraper, webClient, pConnector, promHost, promPort);
        prometheusScraperController= new PrometheusScraperController(PrometheusScraperService);
    }


    private void makeKibanaDashboardController() {
        KibanaDashboardRepo dashboardRepo = new MemoryKibanaDashboardRepo();  // TODO make persistent dash repo?
        WebClient webClient = WebClient.create(
                vertx,
                new WebClientOptions()
                        .setDefaultHost(kibanaHost)
                        .setDefaultPort(kibanaPort)
        );
        KibanaConnector kConnector = new KibanaConnector(webClient);
        KibanaDashboardService service = new KibanaDashboardService(kConnector, dashboardRepo);
        kibanaDashboardController = new KibanaDashboardController(service);
    }

    private void makeAlertsController() {
        PrometheusConnector pConnector = makePrometheusConnector();
        AlertRepo alertRepo = new MemoryAlertRepo(); // TODO make persistent repo?
        TargetRepo targetRepo = new MemoryTargetRepo();
        AlertService alertService = new AlertService(pConnector, alertRepo, targetRepo);
        alertsController = new AlertsController(alertService);
    }

    private void makeTopicController() {
        LogstashConnector lConnector = makeLogstashConnector();
        TopicRepo topicRepo = new MemoryTopicRepo();
        TopicService topicService = new TopicService(lConnector, topicRepo);
        topicController = new TopicController(topicService);
    }

    private void makeControllers() {
        makeExporterController();
        makeDashboardController();
        makeAlertsController();
        makeRVMAgentController();
        makePrometheusScraperController();
        makeTopicController();
        makeKibanaDashboardController();
    }

    private void makeExporterRoutes(Router router) {

        if (exporterController == null) {
            throw new IllegalStateException("Initialization incomplete, missing ExporterController");
        }

        producing(router.get(Paths.Rest.EXP))
                .handler(exporterController::getAllExporters);

        producing(router.post(Paths.Rest.EXP))
                .handler(makeParsingHandler(exporterController::postExporter, ExporterDescription.class));

        producing(router.get(Paths.Rest.ONE_EXP))
                .handler(exporterController::getExporter);

        producing(router.delete(Paths.Rest.ONE_EXP))
                .handler(exporterController::deleteExporter);

        producing(router.put(Paths.Rest.ONE_EXP))
                .handler(makeParsingHandler(exporterController::updateExporter, Exporter.class));
    }

    private void makeDashboardRoutes(Router router) {

        if (dashboardController == null) {
            throw new IllegalStateException("Initialization incomplete, missing dashboard controller");
        }

        producing(router.post(Paths.Rest.DASHBOARD))
                .handler(makeParsingHandler(dashboardController::postDashboard, DashboardDescription.class));

        producing(router.get(Paths.Rest.DASHBOARD))
                .handler(dashboardController::getAllDashboards);

        producing(router.get(Paths.Rest.ONE_DASHBOARD))
                .handler(dashboardController::getDashboard);

        producing(router.delete(Paths.Rest.ONE_DASHBOARD))
                .handler(dashboardController::deleteDashboard);

        producing(router.put(Paths.Rest.ONE_DASHBOARD))
                .handler(makeParsingHandler(dashboardController::updateDashboard, Dashboard.class));
    }


    private void makeKibanaDashboardRoutes(Router router) {

        if (kibanaDashboardController == null) {
            throw new IllegalStateException("Initialization incomplete, missing kibana dashboard controller");
        }

        producing(router.post(Paths.Rest.KIBANA_DASHBOARD))
                .handler(makeParsingHandler(kibanaDashboardController::postDashboard, KibanaDashboardDescription.class));
    }


    private void makeTopicRoutes(Router router) {

        if (topicController == null) {
            throw new IllegalStateException("Initialization incomplete, missing TopicController");
        }

        producing(router.post(Paths.Rest.TOPIC))
                .handler(makeParsingHandler(topicController::postTopic, Topic.class));

        producing(router.get(Paths.Rest.TOPIC))
                .handler(topicController::getAllTopics);

        producing(router.delete(Paths.Rest.TOPIC))
                .handler(makeParsingHandler(topicController::deleteTopic, Topic.class));
    }

    private void makeAlertRoutes(Router router) {
        if (alertsController == null) {
            throw new IllegalStateException("Initialization incomplete, missing alerts controller");
        }
        alertsController.getAllAlerts(producing(router.get(Paths.Rest.ALERT)));
        alertsController.postAlert(producing(router.post(Paths.Rest.ALERT)));
        alertsController.getAlert(producing(router.get(Paths.Rest.ONE_ALERT)));
        alertsController.putAlert(producing(router.put(Paths.Rest.ONE_ALERT)));
        alertsController.deleteAlert(producing(router.delete(Paths.Rest.ONE_ALERT)));

    }

    private void makeRVMAgentRoutes(Router router) {
        if (rvmAgentController == null) {
            throw new IllegalStateException("Initialization incomplete, missing RVMAgent controller");
        }

        rvmAgentController.postRVMAgent(producing(router.post(Paths.Rest.AGENT)));
        rvmAgentController.getRVMAgent(producing(router.get(Paths.Rest.ONE_AGENT)));
        rvmAgentController.deleteRVMAgent(producing(router.delete(Paths.Rest.ONE_AGENT)));
        rvmAgentController.getRVMAgents(producing(router.get(Paths.Rest.AGENT)));
        rvmAgentController.postRVMAgentCommand(producing(router.post(Paths.Rest.COMMAND)));
        rvmAgentController.getRVMAgentCommand(producing(router.get(Paths.Rest.COMMAND_GET)));
        rvmAgentController.postInstallExporter(producing(router.post(Paths.Rest.INSTALL_EXPORTER)));
        rvmAgentController.deleteUninstallExporter(producing(router.delete(Paths.Rest.UNINSTALL_EXPORTER)));
        rvmAgentController.postAddPrometheusCollector(producing(router.post(Paths.Rest.ADD_PROMETHEUS_COLLECTOR)));
        rvmAgentController.deletePrometheusCollector(producing(router.delete(Paths.Rest.DELETE_PROMETHEUS_COLLECTOR)));
        rvmAgentController.getScripts(producing(router.get(Paths.Rest.GET_RESOURCES_SCRIPTS)));
        rvmAgentController.getAgents(producing(router.get(Paths.Rest.GET_RESOURCES_AGENTS)));
    }

    private void makePrometheusScrapeRoutes(Router router) {
        if (prometheusScraperController == null) {
            throw new IllegalStateException("Initialization incomplete, missing PrometheusScrape controller");
        }
        prometheusScraperController.postPrometheusScraper(producing(router.post(Paths.Rest.PROMETHEUS_SCRAPER)));
        prometheusScraperController.deletePrometheusScraper(producing(router.delete(Paths.Rest.DELETE_PROMETHEUS_SCRAPER)));
    }


    private void startPrometheusMQAgent() {

        Properties configPrometheusMQAgent = new Properties();
        configPrometheusMQAgent.put("client.id", "PrometheusMQAgent");
        configPrometheusMQAgent.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        configPrometheusMQAgent.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        configPrometheusMQAgent.put("enable.auto.commit", "true");
        configPrometheusMQAgent.put("group.id", this.config.getProperty("prometheus.PushGateway.group.id"));
        configPrometheusMQAgent.put("bootstrap.servers", this.config.getProperty("kafka.bootstrap.servers"));
        configPrometheusMQAgent.put("prometheus.PushGatewayAddress", this.config.getProperty("prometheus.PushGatewayAddress"));
        configPrometheusMQAgent.put("prometheus.PushGatewayPort", this.config.getProperty("prometheus.PushGatewayPort"));
        configPrometheusMQAgent.put("prometheus.PushGateway.topic", this.config.getProperty("prometheus.PushGateway.topic"));
        PrometheusMQAgent prometheusMQAgent = new PrometheusMQAgent(configPrometheusMQAgent, vertx, concurrentHashMap);
        prometheusMQAgent.start();

    }



    private void handleFailure(RoutingContext ctx) {
        Throwable error = ctx.failure();
        if (!(error instanceof HttpStatusException)) {
            String cause;
            if (error.getCause() != null) {
                cause = String.format(" caused by %s", error.getCause().getMessage());
            } else {
                cause = "";
            }
            log.error("Unexpected error: {}{}", error.getMessage(), cause);
            log.debug("Details:", error);
            respond(ctx, 500, new ErrorResponse(
                    "500",
                    "Internal Server Error",
                    "Unexpected error"
            ));
        } else {
            HttpStatusException httpError = (HttpStatusException) error;
            respond(
                    ctx,
                    httpError.getStatusCode(),
                    new ErrorResponse(
                            String.valueOf(httpError.getStatusCode()),
                            HttpResponseStatus.valueOf(httpError.getStatusCode()).reasonPhrase(),
                            httpError.getPayload()
                    )
            );
        }
    }

    @Override
    public void start() {
        readConfig();
        startPrometheusMQAgent();
        makePrometheusConnector();

        makeControllers();
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create()).failureHandler(this::handleFailure);
        makeExporterRoutes(router);
        makeDashboardRoutes(router);
        makeAlertRoutes(router);
        makeRVMAgentRoutes(router);
        makePrometheusScrapeRoutes(router);
        vertx.createHttpServer().requestHandler(router::accept).listen(port);
    }


    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(MainVerticle.class.getName());
    }
}
