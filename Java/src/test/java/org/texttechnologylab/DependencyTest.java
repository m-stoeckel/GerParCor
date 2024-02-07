package org.texttechnologylab;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.impl.PrimitiveAnalysisEngine_impl;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.texttechnologylab.engine.DummyEngine;
import org.texttechnologylab.parliament.duui.mdd.data.DocumentDataPoint;
import org.texttechnologylab.parliament.duui.mdd.data.EdgeDataPoint;

import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionMethod;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.PUNCT;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.ROOT;

public class DependencyTest {

    @Test
    public void testWithValue() {
        try {
            String pOutput = System.getProperty("output", "target/output/");

            TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                    .createTypeSystemDescriptionFromPath(
                            "src/test/resources/TypeSystem.xml");
            JCas jCas = JCasFactory.createJCas("src/test/resources/test.xmi", typeSystemDescription);

            AnalysisEngine engine = createEngine(
                    DummyEngine.class,
                    DummyEngine.PARAM_TARGET_LOCATION,
                    pOutput,
                    DummyEngine.PARAM_OVERWRITE,
                    true,
                    DummyEngine.PARAM_COMPRESSION,
                    CompressionMethod.NONE,
                    DummyEngine.PARAM_FAIL_ON_ERROR,
                    true);

            engine.process(jCas);

            final List<Integer> expectedDistances = List.of(3, 2, 1, 1, 3, 2, 1, 4);
            final int expectedNumberOfSyntacticLinks = 10;
            final int expectedSentenceLength = 8;
            final int expectedRootDistance = 5;
            final double expectedMDD = 2.125;

            try {
                Field field = PrimitiveAnalysisEngine_impl.class.getDeclaredField("mAnalysisComponent");
                field.setAccessible(true);

                AnalysisComponent component = (AnalysisComponent) field.get(engine);
                DummyEngine dummyEngine = (DummyEngine) component;

                DocumentDataPoint documentDataPoint = dummyEngine.documentDataPoint;
                EdgeDataPoint sentenceDataPoint = (EdgeDataPoint) documentDataPoint.getSentences().get(0);
                List<Integer> dependencyDistances = sentenceDataPoint.getDependencyDistances();

                System.out.println("Tokens:");
                ArrayList<Token> tokens = new ArrayList<>(JCasUtil.select(jCas, Token.class));
                for (int i = 0; i < tokens.size() - 1; i++) {
                    Token token = tokens.get(i);
                    System.out.printf("  %d: '%s' (%d, %d)\n", i, token.getCoveredText(), token.getBegin(),
                            token.getEnd());
                }

                ArrayList<Dependency> dependencies = new ArrayList<>(JCasUtil.select(jCas, Dependency.class));
                dependencies.sort(Comparator.comparingInt(o -> o.getDependent().getBegin()));

                int counter = 0;
                System.out.println("Dependencies:");
                for (Dependency dep : dependencies) {
                    Token dependent = dep.getDependent();
                    Token governor = dep.getGovernor();
                    String dependencyType = dep.getDependencyType();
                    if (dep instanceof PUNCT || dependencyType.equalsIgnoreCase("PUNCT"))
                        continue;

                    System.out.printf(
                            "  %-6s %d -> %-6s %d = %d\n",
                            dependent.getCoveredText(),
                            tokens.indexOf(dependent) + 1,
                            dep instanceof ROOT ? "ROOT" : governor.getCoveredText(),
                            tokens.indexOf(governor) + 1,
                            dep instanceof ROOT ? 0 : dependencyDistances.get(counter++));
                }

                Assertions.assertEquals(expectedDistances, dependencyDistances);
                Assertions.assertEquals(expectedSentenceLength, sentenceDataPoint.getSentenceLength());
                Assertions.assertEquals(expectedNumberOfSyntacticLinks, sentenceDataPoint.numberOfSyntacticLinks);
                Assertions.assertEquals(expectedRootDistance, sentenceDataPoint.rootDistance);
                Assertions.assertEquals(expectedMDD, sentenceDataPoint.mdd());
            } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException | SecurityException e) {
                throw new RuntimeException(e);
            }

            Assertions.assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail();
        }
    }

}
