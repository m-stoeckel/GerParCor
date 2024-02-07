import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.nio.file.Path;
import java.util.Objects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.connection.mongodb.MongoDBConfig;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIPipelineComponent;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIUIMADriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.DUUIAsynchronousProcessor;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.texttechnologylab.parliament.duui.DUUIGerParCorReader;
import org.texttechnologylab.parliament.duui.mdd.engine.DependencyDistanceEngine;

import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionMethod;

public class Dependency {

    @Test
    public void GerParCor() {
        try {
            String pConfig = System.getProperty("config");
            if (Objects.isNull(pConfig)) {
                System.out.println("Please provide a MongoDB configuration file.");
                Assertions.fail();
            }

            String pFilter = System.getProperty("filter", "{}");
            int pScale = Integer.parseInt(System.getProperty("scale", "16"));
            int pPoolsize = Integer.parseInt(System.getProperty("poolsize", "24"));

            String pOutput = System.getProperty("output", "");
            if (Objects.isNull(pOutput)) {
                System.out.println("Please provide an output path.");
                Assertions.fail();
            }

            boolean pOverwrite = Boolean.parseBoolean(System.getProperty("overwrite", "false"));
            CompressionMethod pCompression = CompressionMethod
                    .valueOf(System.getProperty("compression", "NONE"));

            boolean pFailOnError = Boolean.parseBoolean(System.getProperty("failOnError", "false"));
            boolean pFixDateYear = Boolean.parseBoolean(System.getProperty("fixDateYear", "true"));
            boolean pMkDirs = Boolean.parseBoolean(System.getProperty("mkdirs", "true"));

            System.out.printf(
                    "Settings:\n" +
                            "  pConfig:      %s\n" +
                            "  pOutput:      %s\n" +
                            "  pFilter:      %s\n" +
                            "  pScale:       %d\n" +
                            "  pPoolsize:    %d\n" +
                            "  pOverwrite:   %b\n" +
                            "  pCompression: %s\n" +
                            "  pFailOnError: %b\n" +
                            "  pFixDateYear: %b\n" +
                            "  pMkDirs:      %b\n",
                    pConfig,
                    pOutput,
                    pFilter,
                    pScale,
                    pPoolsize,
                    pOverwrite,
                    pCompression,
                    pFailOnError,
                    pFixDateYear,
                    pMkDirs);

            Path outputPath = Path.of(pOutput);
            if (!outputPath.toFile().exists() && pMkDirs) {
                outputPath.toFile().mkdirs();
            }

            MongoDBConfig mongoDbConfig = new MongoDBConfig(pConfig);
            System.out.printf("MongoDBConfig:\n  %s\n", mongoDbConfig);

            DUUIAsynchronousProcessor processor = new DUUIAsynchronousProcessor(
                    new DUUIGerParCorReader(mongoDbConfig, pFilter));

            DUUIComposer composer = new DUUIComposer()
                    .withSkipVerification(true)
                    .withWorkers(pScale)
                    .withCasPoolsize(pPoolsize)
                    .withLuaContext(new DUUILuaContext().withJsonLibrary());

            DUUIUIMADriver uimaDriver = new DUUIUIMADriver();
            composer.addDriver(uimaDriver);

            DUUIPipelineComponent dependency = new DUUIUIMADriver.Component(
                    createEngineDescription(
                            DependencyDistanceEngine.class,
                            DependencyDistanceEngine.PARAM_TARGET_LOCATION,
                            pOutput,
                            DependencyDistanceEngine.PARAM_OVERWRITE,
                            pOverwrite,
                            DependencyDistanceEngine.PARAM_COMPRESSION,
                            pCompression,
                            DependencyDistanceEngine.PARAM_FAIL_ON_ERROR,
                            pFailOnError,
                            DependencyDistanceEngine.PARAM_FIX_DATE_YEAR,
                            pFixDateYear))
                    .withScale(pScale)
                    .build();
            composer.add(dependency);

            composer.run(processor, "mDD");
            composer.shutdown();

            Assertions.assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail();
        }
    }

}
