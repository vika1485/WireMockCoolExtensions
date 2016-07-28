package com.stubs.cool_extensions.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.FatalStartupException;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.standalone.CommandLineOptions;
import com.github.tomakehurst.wiremock.standalone.MappingsLoader;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.stubbing.StubMappings;
import com.stubs.cool_extensions.transformer.AbstractTransformer;
import com.stubs.cool_extensions.transformer.CoolExtensionsTransformer;
import com.typesafe.config.ConfigFactory;

import static com.github.tomakehurst.wiremock.WireMockServer.FILES_ROOT;
import static com.github.tomakehurst.wiremock.WireMockServer.MAPPINGS_ROOT;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.http.RequestMethod.ANY;
import static java.lang.System.out;

public class WireMockServerRunner {
    private static String TRANSFORM_CONFIG = "conf/transformer";

    private static final String BANNER = " /$$      /$$ /$$                     /$$      /$$                     /$$      \n"
            + "| $$  /$ | $$|__/                    | $$$    /$$$                    | $$      \n"
            + "| $$ /$$$| $$ /$$  /$$$$$$   /$$$$$$ | $$$$  /$$$$  /$$$$$$   /$$$$$$$| $$   /$$\n"
            + "| $$/$$ $$ $$| $$ /$$__  $$ /$$__  $$| $$ $$/$$ $$ /$$__  $$ /$$_____/| $$  /$$/\n"
            + "| $$$$_  $$$$| $$| $$  \\__/| $$$$$$$$| $$  $$$| $$| $$  \\ $$| $$      | $$$$$$/ \n"
            + "| $$$/ \\  $$$| $$| $$      | $$_____/| $$\\  $ | $$| $$  | $$| $$      | $$_  $$ \n"
            + "| $$/   \\  $$| $$| $$      |  $$$$$$$| $$ \\/  | $$|  $$$$$$/|  $$$$$$$| $$ \\  $$\n"
            + "|__/     \\__/|__/|__/       \\_______/|__/     |__/ \\______/  \\_______/|__/  \\__/";

    static {
        System.setProperty("org.mortbay.log.class", "com.github.tomakehurst.wiremock.jetty.LoggerAdapter");
    }

    private WireMockServer wireMockServer;

    public void run(String... args) {
        CommandLineOptions options = new CommandLineOptions(args);
        System.out.println(options.portNumber());
        if (options.help()) {
            out.println(options.helpText());
            return;
        }

        FileSource fileSource = options.filesRoot();
        fileSource.createIfNecessary();
        FileSource filesFileSource = fileSource.child(FILES_ROOT);
        filesFileSource.createIfNecessary();
        FileSource mappingsFileSource = fileSource.child(MAPPINGS_ROOT);
        mappingsFileSource.createIfNecessary();
        WireMockConfiguration options1 = wireMockConfig()
                .port(8980)
                .extensions(getTransformer()).
                        usingFilesUnderClasspath("mappingsResponse");
        wireMockServer = new WireMockServer(options1);
        wireMockServer.enableRecordMappings(mappingsFileSource, filesFileSource);

        if (options.specifiesProxyUrl()) {
            addProxyMapping(options.proxyUrl());
        }

        try {

            wireMockServer.start();
            ;
            out.println(BANNER);
            out.println();
            out.println(options);
        } catch (FatalStartupException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private void addProxyMapping(final String baseUrl) {
        wireMockServer.loadMappingsUsing(new MappingsLoader() {
            public void loadMappingsInto(StubMappings stubMappings) {
                RequestPattern requestPattern = new RequestPattern(ANY);
                requestPattern.setUrlPattern(".*");
                ResponseDefinition responseDef = new ResponseDefinition();
                responseDef.setProxyBaseUrl(baseUrl);

                StubMapping proxyBasedMapping = new StubMapping(requestPattern, responseDef);
                proxyBasedMapping.setPriority(10); // Make it low priority so that existing stubs will take precedence
                stubMappings.addMapping(proxyBasedMapping);
            }
        });
    }


    private <T extends AbstractTransformer> Class<T> getTransformer() {
        Class retval = CoolExtensionsTransformer.class;

        try {
            retval = Class.forName(ConfigFactory.load(TRANSFORM_CONFIG)
                    .withFallback(ConfigFactory.load("conf/defaults")).getString("wiremock.transformer")).newInstance().getClass();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retval;
    }

    public void stop() {
        wireMockServer.stop();
    }

    public boolean isRunning() {
        return wireMockServer.isRunning();
    }

    public int port() {
        return wireMockServer.port();
    }

    public static void main(String... args) {
        new WireMockServerRunner().run(args);
    }
}