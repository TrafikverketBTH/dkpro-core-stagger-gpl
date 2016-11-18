package de.tudarmstadt.ukp.dkpro.core.stagger;

import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.component.initialize.ConfigurationParameterInitializer;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.morph.MorphologicalFeaturesParser;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.resources.CasConfigurableProviderBase;
import de.tudarmstadt.ukp.dkpro.core.api.resources.MappingProvider;
import de.tudarmstadt.ukp.dkpro.core.api.resources.MappingProviderFactory;
import de.tudarmstadt.ukp.dkpro.core.api.resources.ModelProviderBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import se.su.ling.stagger.TagNameException;
import se.su.ling.stagger.TagSet;
import se.su.ling.stagger.TaggedToken;
import se.su.ling.stagger.Tagger;

@TypeCapability(
        inputs = {
                "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
                "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence" },
        outputs = {"de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS", 
                "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma",
                "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures"})
public class StockholmPosTagger
    extends JCasAnnotator_ImplBase
{
    /**
     * Write lemma information.
     *
     * Default: {@code true}
     */
    public static final String PARAM_WRITE_LEMMA = ComponentParameters.PARAM_WRITE_LEMMA;
    @ConfigurationParameter(name=PARAM_WRITE_LEMMA, mandatory=false, defaultValue="true")
    private boolean writeLemma;
    
    /**
     * Write morphological features information.
     *
     * Default: {@code true}
     */
    public static final String PARAM_WRITE_MORPH = ComponentParameters.PARAM_WRITE_MORPH;
    @ConfigurationParameter(name=PARAM_WRITE_MORPH, mandatory=false, defaultValue="true")
    private boolean writeMorph;
    
    private CasConfigurableProviderBase<Tagger> modelProvider;
    private MappingProvider posMappingProvider;
    private MorphologicalFeaturesParser featuresParser;
    
    @Override
    public void initialize(UimaContext aContext)
        throws ResourceInitializationException
    {
        super.initialize(aContext);
        ConfigurationParameterInitializer.initialize(this, aContext);
                
        modelProvider = new ModelProviderBase<Tagger>(this, "stagger", "tagger") {
            @Override
            protected Tagger produceResource(InputStream aStream) throws IOException
            {
                ObjectInputStream modelReader = new ObjectInputStream(aStream);
                Tagger tagger = null; 
                
                try {
                    tagger = (Tagger) modelReader.readObject();
                }
                catch(ClassNotFoundException e) {
                    throw new IOException(e);
                }
                finally {
                    modelReader.close();
                }
                
                return tagger;
            }
        };
        
        posMappingProvider = MappingProviderFactory.createPosMappingProvider(null, "suc", "sv");
        featuresParser = new MorphologicalFeaturesParser(this, modelProvider);
    }
    

    @Override
    public void process(JCas aJCas)
        throws AnalysisEngineProcessException
    {
        CAS cas = aJCas.getCas();

        modelProvider.configure(cas);
        posMappingProvider.configure(cas);
        featuresParser.configure(cas);
        
        for (Sentence sentence : select(aJCas, Sentence.class)) {
            List<Token> tokens = selectCovered(aJCas, Token.class, sentence);
            
            TaggedToken[] staggerTokens = new TaggedToken[tokens.size()];
            int i = 0;
            for(Token t : tokens) {
                se.su.ling.stagger.Token staggerToken = 
                        new se.su.ling.stagger.Token(Integer.parseInt(t.getId()), t.getCoveredText(), t.getBegin());
                String id = aJCas.getSofa().getSofaID() + ":" + staggerToken.offset;
                staggerTokens[i] = new TaggedToken(staggerToken, id);
                i++;
            }
            
            Tagger tagger = modelProvider.getResource();
            TaggedToken[] taggedStaggerTokens = tagger.tagSentence(staggerTokens, true, false);
            TagSet posTagSet = tagger.getTaggedData().getPosTagSet();
            
            i = 0;
            for(Token token : tokens) {
                TaggedToken taggedStaggerToken = taggedStaggerTokens[i];
                int tokenBegin = token.getBegin();
                int tokenEnd = token.getEnd();
                
                if(writeLemma) {
                    Lemma lemmaAnno = new Lemma(aJCas, tokenBegin, tokenEnd);
                    lemmaAnno.setValue(taggedStaggerToken.lf);
                    lemmaAnno.addToIndexes();
                    token.setLemma(lemmaAnno);
                }
                
                String[] pos = null;
                if(taggedStaggerToken.posTag >= 0 && posTagSet != null) {
                    try {
                        pos = posTagSet.getTagName(taggedStaggerToken.posTag).split("\\|", 2);
                        if(pos != null) {
                            Type posTag = posMappingProvider.getTagType(pos[0]);
                            POS posAnno = (POS) cas.createAnnotation(posTag, tokenBegin, tokenEnd);
                            posAnno.setPosValue(pos[0]);
                            posAnno.addToIndexes();
                            token.setPos(posAnno);
                            
                            if(writeMorph && pos.length > 1) {
                                MorphologicalFeatures analysis = featuresParser.parse(aJCas, token, pos[1]);
                                token.setMorph(analysis);
                            }
                        }
                    }
                    catch(TagNameException e) {
                        e.printStackTrace();
                    }
                }
                
                i++;

            } 
        }
    }

}
