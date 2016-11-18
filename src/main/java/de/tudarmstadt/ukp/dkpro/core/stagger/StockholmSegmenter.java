package de.tudarmstadt.ukp.dkpro.core.stagger;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.LanguageCapability;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.SegmenterBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import se.su.ling.stagger.SwedishTokenizer;
import se.su.ling.stagger.Tokenizer;

@LanguageCapability({"sv"})
public class StockholmSegmenter
    extends SegmenterBase
{

    @Override
    protected void process(JCas aJCas, String aText, int aZoneBegin)
        throws AnalysisEngineProcessException
    {
        Tokenizer aTok = new SwedishTokenizer(new StringReader(aText));
        ArrayList<se.su.ling.stagger.Token> sentence;
        
        try {
            while((sentence = aTok.readSentence()) != null) {
                if(isWriteSentence() && !sentence.isEmpty()) {
                    int aBegin = sentence.get(0).offset;
                    se.su.ling.stagger.Token lastToken = sentence.get(sentence.size() - 1);
                    int aEnd = lastToken.offset + lastToken.value.length();
                    
                    createSentence(aJCas, aBegin, aEnd);
                }
                
                if(isWriteToken() && !sentence.isEmpty()) {
                    List<Token> casTokens = new ArrayList<Token>();
                    for(se.su.ling.stagger.Token aToken : sentence) {
                        int aBegin = aToken.offset;
                        int aEnd = aToken.offset + aToken.value.length();
                        Token t = createToken(aJCas, aBegin, aEnd);
                        t.setId(String.valueOf(aToken.type));
                        casTokens.add(t);
                    }
                }
            }
        } 
        catch(IOException e) {
            throw new AnalysisEngineProcessException(e);
        }

    }

}
