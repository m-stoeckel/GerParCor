package org.texttechnologylab.parliament.duui.mdd.data;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.apache.commons.codec.binary.Hex;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.texttechnologylab.annotation.DocumentAnnotation;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

public class DocumentDataPoint {

    protected final TreeMap<String, String> documentAnnotation;
    protected final TreeMap<String, String> documentMetaData;
    protected final ArrayList<SentenceDataPoint> sentences;

    public DocumentDataPoint(DocumentAnnotation documentAnnotation, DocumentMetaData documentMetaData) {
        this.documentAnnotation = featureMap(documentAnnotation);
        this.documentMetaData = featureMap(documentMetaData);
        this.sentences = new ArrayList<>();
    }

    public static TreeMap<String, String> featureMap(AnnotationBase annotation) {
        TreeMap<String, String> map = new TreeMap<>();
        for (Feature feature : annotation.getType().getFeatures()) {
            try {
                String featureValueAsString = annotation.getFeatureValueAsString(feature);
                if (Objects.nonNull(featureValueAsString))
                    map.put(feature.getShortName(), featureValueAsString);
            } catch (CASRuntimeException ignored) {
            }
        }
        return map;
    }

    public static DocumentDataPoint fromJCas(JCas jCas) {
        DocumentAnnotation documentAnnotation = JCasUtil.selectSingle(jCas, DocumentAnnotation.class);
        DocumentMetaData documentMetaData = DocumentMetaData.get(jCas);
        return new DocumentDataPoint(documentAnnotation, documentMetaData);
    }

    public void add(SentenceDataPoint sentenceDataPoint) {
        this.sentences.add(sentenceDataPoint);
    }

    public String getMetaHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            this.documentMetaData
                    .forEach((k, v) -> digest.update(String.join(":", k, v).getBytes(StandardCharsets.UTF_8)));
            this.documentAnnotation
                    .forEach((k, v) -> digest.update(String.join(":", k, v).getBytes(StandardCharsets.UTF_8)));
            String metaHash = Hex.encodeHexString(digest.digest());
            return metaHash;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> getDocumentAnnotation() {
        return this.documentAnnotation;
    }

    public Map<String, String> getDocumentMetaData() {
        return this.documentMetaData;
    }

    public List<SentenceDataPoint> getSentences() {
        return this.sentences;
    }
}
