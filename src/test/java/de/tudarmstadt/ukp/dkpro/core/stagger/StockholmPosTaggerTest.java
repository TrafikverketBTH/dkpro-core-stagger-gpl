package de.tudarmstadt.ukp.dkpro.core.stagger;


import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static de.tudarmstadt.ukp.dkpro.core.testing.AssertAnnotations.assertMorph;
import static de.tudarmstadt.ukp.dkpro.core.testing.AssertAnnotations.assertToken;
import static de.tudarmstadt.ukp.dkpro.core.testing.AssertAnnotations.assertLemma;
import static de.tudarmstadt.ukp.dkpro.core.testing.AssertAnnotations.assertPOS;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.jcas.JCas;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

public class StockholmPosTaggerTest
{
    @Test
    public void test() throws Exception
    {
        JCas jcas = runTest("För telefonrådfrågning betalar försäkringskassan 4 kronor till sjukvårdshuvudmannen.");
        String[] tokens = { "För", "telefonrådfrågning", "betalar", "försäkringskassan", "4", "kronor", "till", "sjukvårdshuvudmannen", "." };
        String[] lemmas = { "för", "telefonrådfrågning", "betala", "försäkringskassa", "4", "krona", "till", "sjukvårdshuvudman", "." };
        String[] posOriginal = { "PP", "NN", "VB", "NN", "RG", "NN", "PP", "NN", "MAD" };
        //Mapping retrieved from dkpro-core-api-lexmorph-asl/src/main/resources/desc/type/POS.xml
        String[] posMapped = { "ADP", "NOUN", "VERB", "NOUN", "NUM", "NOUN", "ADP", "NOUN", "PUNCT" };
        String[] morph = { 
                "[  4, 22]     -     -  Nom  Ind    -   Com    -    -  Sing      -  -    -    -    -     -      -     - telefonrådfrågning (UTR|SIN|IND|NOM)", 
                "[ 23, 30]     -     -    -    -    -     -    -    -     -      -  -    -    -    -  Pres      -   Act betalar (PRS|AKT)", 
                "[ 31, 48]     -     -  Nom  Def    -   Com    -    -  Sing      -  -    -    -    -     -      -     - försäkringskassan (UTR|SIN|DEF|NOM)", 
                "[ 49, 50]     -     -  Nom    -    -     -    -    -     -      -  -    -    -    -     -      -     - 4 (NOM)", 
                "[ 51, 57]     -     -  Nom  Ind    -   Com    -    -  Plur      -  -    -    -    -     -      -     - kronor (UTR|PLU|IND|NOM)", 
                "[ 63, 83]     -     -  Nom  Def    -   Com    -    -  Sing      -  -    -    -    -     -      -     - sjukvårdshuvudmannen (UTR|SIN|DEF|NOM)"
        };
       
        
        assertToken(tokens, select(jcas, Token.class));
        assertLemma(lemmas, select(jcas, Lemma.class));
        assertPOS(posMapped, posOriginal, select(jcas, POS.class));
        assertMorph(morph, select(jcas, MorphologicalFeatures.class));
    }
    
    private JCas runTest(String aText) throws Exception
    {            
        AnalysisEngineDescription segmenter = createEngineDescription(StockholmSegmenter.class); 
        AnalysisEngineDescription parser = createEngineDescription(StockholmPosTagger.class);

        AnalysisEngine engine = createEngine(createEngineDescription(segmenter, parser));

        JCas jcas = engine.newJCas();
        jcas.setDocumentLanguage("sv");
        jcas.setDocumentText(aText); 
        engine.process(jcas);

        return jcas;
    }

    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
