package org.texttechnologylab.parliament.duui;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.texttechnologylab.annotation.AnnotatorMetaData;

import java.util.HashSet;
import java.util.Set;

public class RemoveAnnotations extends JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {

        Set<Annotation> tAnnotation = new HashSet<>();

        JCasUtil.select(jCas, Annotation.class).forEach(a->{
           if(!(a instanceof DocumentMetaData)) {
               tAnnotation.add(a);
           }
        });


        tAnnotation.stream().forEach(a->{
            a.removeFromIndexes();
        });

    }
}
