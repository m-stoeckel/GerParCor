package org.texttechnologylab.parliament.duui.mdd.engine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.texttechnologylab.parliament.duui.mdd.data.DocumentDataPoint;
import org.texttechnologylab.parliament.duui.mdd.data.SentenceDataPoint;

import com.google.gson.Gson;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.PUNCT;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.ROOT;
import io.azam.ulidj.ULID;

public class DependencyDistanceEngine extends JCasFileWriter_ImplBase {

    public static final String PARAM_FAIL_ON_ERROR = "pFailOnError";

    @ConfigurationParameter(name = PARAM_FAIL_ON_ERROR, mandatory = false, defaultValue = "false")
    protected Boolean pFailOnError;

    public static final String PARAM_ULID_SUFFIX = "pUlidSuffix";

    @ConfigurationParameter(name = PARAM_ULID_SUFFIX, mandatory = false, defaultValue = "false")
    protected Boolean pUlidSuffix;

    public static final String PARAM_FIX_DATE_YEAR = "pFixDateYear";

    @ConfigurationParameter(name = PARAM_FIX_DATE_YEAR, mandatory = false, defaultValue = "true")
    protected Boolean pFixDateYear;

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        try {
            final DocumentDataPoint documentDataPoint = DocumentDataPoint.fromJCas(jCas);

            String dateYear = documentDataPoint.getDocumentAnnotation().getOrDefault("dateYear", "0000");
            if (pFixDateYear) {
                try {
                    int iDateYear = Integer.parseInt(dateYear);
                    dateYear = (iDateYear > 0 && iDateYear < 200) ? String.valueOf(iDateYear + 1900) : dateYear;
                    documentDataPoint.getDocumentAnnotation().put("dateYear", dateYear);
                } catch (NumberFormatException e) {
                    getLogger().error(String.format("Could not parse dateYear '%s': %s", dateYear, e.getMessage()));
                    if (pFailOnError)
                        throw new AnalysisEngineProcessException(e);
                }
            }

            String metaHash = documentDataPoint.getMetaHash();

            String outputFile = String.join("/", dateYear, metaHash);
            if (pUlidSuffix) {
                outputFile = String.join("-", outputFile, ULID.random());
            }

            try {
                // Try to get the output stream _before_ processing the document
                // as we will get an IOException if the target file already exists
                NamedOutputStream outputStream = getOutputStream(outputFile, ".json");

                try {
                    processDocument(jCas, documentDataPoint);
                } catch (Exception e) {
                    // Unexpected: processDocument() is pretty safe, so something bad happened
                    throw new AnalysisEngineProcessException(
                            "Error while processing the document. This should not happen!", null, e);
                }

                try {
                    save(documentDataPoint, outputStream);
                } catch (IOException e) {
                    // Unexpected: We could not write to the output stream?
                    throw new AnalysisEngineProcessException("Could not save document data point to output stream.",
                            null, e);
                }
            } catch (IOException e) {
                // Expected: getOutputStream() failed, most likely because the target file
                // already exists
                getLogger().error(e.getMessage());
                if (pFailOnError)
                    throw new AnalysisEngineProcessException(e);
            }
        } catch (AnalysisEngineProcessException e) {
            // Something unexpected happened or an execption was passed on because
            // pFailOnError is true
            getLogger().error(e.getMessage());
            e.printStackTrace();
            if (pFailOnError) {
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void processDocument(JCas jCas, final DocumentDataPoint documentDataPoint) {
        final ArrayList<Sentence> sentences = new ArrayList<>(new ArrayList<>(JCasUtil.select(jCas, Sentence.class)));
        final HashMap<Sentence, Collection<Token>> tokenMap = new HashMap<>(
                JCasUtil.indexCovered(jCas, Sentence.class, Token.class));
        final HashMap<Sentence, Collection<Dependency>> dependencyMap = new HashMap<>(
                JCasUtil.indexCovered(jCas, Sentence.class, Dependency.class));

        for (Sentence sentence : sentences) {
            if (!sentenceIsValid(sentence, tokenMap))
                continue;

            final Collection<Dependency> dependencies = dependencyMap.get(sentence);
            if (dependencies == null || dependencies.size() < 2) {
                getLogger().debug(String.format("Skipping due to dependencies: %s", dependencies));
                continue;
            }

            documentDataPoint.add(processDependencies(new ArrayList<>(dependencyMap.get(sentence))));
        }
    }

    private boolean sentenceIsValid(Sentence sentence, HashMap<Sentence, Collection<Token>> tokenMap) {
        if (!tokenMap.containsKey(sentence)) {
            getLogger().debug(String.format("Sentence not in tokenMap: '%s'", sentence.toString()));
            return false;
        }

        final Collection<Token> tokens = tokenMap.get(sentence);
        if (tokens == null) {
            getLogger().debug(String.format("Tokens are null for sentence: '%s'", sentence.toString()));
            return false;
        }

        if (tokens.size() < 3) {
            getLogger().debug(String.format("Sentence too short: '%s'", sentence.toString()));
            return false;
        }
        return true;
    }

    private SentenceDataPoint processDependencies(final ArrayList<Dependency> dependencies) {
        dependencies.sort(Comparator.comparingInt(o -> o.getDependent().getBegin()));
        ArrayList<Token> tokens = dependencies
                .stream()
                .flatMap(d -> Stream.of(d.getGovernor(), d.getDependent()))
                .distinct()
                .sorted(Comparator.comparingInt(Annotation::getBegin))
                .collect(Collectors.toCollection(ArrayList::new));

        int rootDistance = -1;
        int numberOfSyntacticLinks = 0;
        SentenceDataPoint sentenceDataPoint = createSentenceDataPoint();
        for (Dependency dependency : dependencies) {
            numberOfSyntacticLinks++;
            String dependencyType = dependency.getDependencyType();
            if (dependency instanceof PUNCT || dependencyType.equalsIgnoreCase("PUNCT")) {
                continue;
            }
            Token governor = dependency.getGovernor();
            Token dependent = dependency.getDependent();
            if (dependency instanceof ROOT || governor == dependent || dependencyType.equalsIgnoreCase("ROOT")) {
                rootDistance = numberOfSyntacticLinks;
                continue;
            }

            int dist = Math.abs(tokens.indexOf(governor) - tokens.indexOf(dependent));

            sentenceDataPoint.add(dist);
        }
        sentenceDataPoint.rootDistance = rootDistance;
        sentenceDataPoint.numberOfSyntacticLinks = numberOfSyntacticLinks;
        return sentenceDataPoint;
    }

    protected SentenceDataPoint createSentenceDataPoint() {
        return new SentenceDataPoint();
    }

    protected static void save(DocumentDataPoint dataPoints, OutputStream outputStream) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            String json = new Gson().toJson(dataPoints);
            writer.write(json);
        }
    }
}
