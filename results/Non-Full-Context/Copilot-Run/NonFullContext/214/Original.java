/*
  * Anserini: A Lucene toolkit for reproducible information retrieval research
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package io.anserini.index;
 
 import io.anserini.analysis.AnalyzerUtils;
 import io.anserini.search.SearchArgs;
 import io.anserini.search.query.BagOfWordsQueryGenerator;
 import io.anserini.search.query.PhraseQueryGenerator;
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.index.DirectoryReader;
 import org.apache.lucene.index.FieldInfo;
 import org.apache.lucene.index.FieldInfos;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.MultiTerms;
 import org.apache.lucene.index.PostingsEnum;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.Terms;
 import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.ConstantScoreQuery;
 import org.apache.lucene.search.DocIdSetIterator;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.ScoreDoc;
 import org.apache.lucene.search.TermQuery;
 import org.apache.lucene.search.TopDocs;
 import org.apache.lucene.search.TotalHitCountCollector;
 import org.apache.lucene.search.similarities.BM25Similarity;
 import org.apache.lucene.search.similarities.Similarity;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.store.FSDirectory;
 import org.apache.lucene.util.BytesRef;
 import org.kohsuke.args4j.CmdLineException;
 import org.kohsuke.args4j.CmdLineParser;
 import org.kohsuke.args4j.Option;
 import org.kohsuke.args4j.ParserProperties;
 
 import java.io.IOException;
 import java.nio.file.Paths;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
 /**
  * Class containing a bunch of static helper methods for accessing a Lucene inverted index.
  * This class provides a lot of functionality that is exposed in Python via Pyserini.
  */
 public class IndexReaderUtils {
 
   /**
    * An individual posting in a postings list. Note that this class is used primarily for inspecting
    * the index, and not meant for actual searching.
    */
   public static class Posting {
     private final int docId;
     private final int termFreq;
     private final int[] positions;
 
     /**
      * Constructor wrapping a {@link PostingsEnum} from Lucene.
      *
      * @param postingsEnum posting from Lucene
      * @throws IOException if error encountered reading information from the posting
      */
     public Posting(PostingsEnum postingsEnum) throws IOException {
       this.docId = postingsEnum.docID();
       this.termFreq = postingsEnum.freq();
       this.positions = new int[this.termFreq];
       for (int j=0; j < this.termFreq; j++) {
         this.positions[j] = postingsEnum.nextPosition();
       }
     }
 
     /**
      * Returns the term frequency stored in this posting.
      *
      * @return the term frequency stored in this posting
      */
     public int getTF() {
       return this.termFreq;
     }
 
     /**
      * Returns the internal Lucene docid associated with this posting.
      *
      * @return the internal Lucene docid associated with this posting
      */
     public int getDocid() {
       return this.docId;
     }
 
     /**
      * Returns the positions in the document where this term is found.
      *
      * @return the positions in the document where this term is found
      */
     public int[] getPositions() {
       return this.positions;
     }
   }
 
   /**
    * A term from the index. Note that this class is used primarily for inspecting the index, not meant for actual
    * searching.
    */
   public static class IndexTerm {
     private final int docFreq;
     private final String term;
     private final long totalTermFreq;
 
     /**
      * Constructor wrapping a {@link TermsEnum} from Lucene.
      *
      * @param term Lucene {@link TermsEnum} to wrap
      * @throws IOException if any errors are encountered
      */
     public IndexTerm(TermsEnum term) throws IOException {
       this.docFreq = term.docFreq();
       this.term = term.term().utf8ToString();
       this.totalTermFreq = term.totalTermFreq();
     }
 
     /**
      * Returns the number of documents containing the current term.
      *
      * @return the number of documents containing the current term
      */
     public int getDF() {
       return this.docFreq;
     }
 
     /**
      * Returns the string representation of the current term.
      *
      * @return the string representation of the current term
      */
     public String getTerm() {
       return this.term;
     }
 
     /**
      * Returns the total number of occurrences of the current term across all documents.
      *
      * @return the total number of occurrences of the current term across all documents
      */
     public long getTotalTF() {
       return this.totalTermFreq;
     }
   }
 
   /**
    * Creates an {@link IndexReader} given a path.
    *
    * @param path index path
    * @return index reader
    * @throws IOException if any errors are encountered
    */
   public static IndexReader getReader(String path) throws IOException {
     Directory dir = FSDirectory.open(Paths.get(path));
     return DirectoryReader.open(dir);
   }
 
   /**
    * Returns count information on a term or a phrase.
    *
    * @param reader index reader
    * @param termStr term
    * @return df (+cf if only one term) of the phrase using default analyzer
    * @throws IOException if error encountered during access to index
    */
   public static Map<String, Long> getTermCounts(IndexReader reader, String termStr)
       throws IOException {
     Analyzer analyzer = IndexCollection.DEFAULT_ANALYZER;
     return getTermCountsWithAnalyzer(reader, termStr, analyzer);
   }
 
   /**
    * Returns count information on a term or a phrase.
    *
    * @param reader index reader
    * @param termStr term
    * @param analyzer analyzer to use
    * @return df (+cf if only one term) of the phrase
    * @throws IOException if error encountered during access to index
    */
   public static Map<String, Long> getTermCountsWithAnalyzer(IndexReader reader, String termStr, Analyzer analyzer)
       throws IOException {
     if (AnalyzerUtils.analyze(analyzer, termStr).size() > 1) {
       Query query = new PhraseQueryGenerator().buildQuery(IndexArgs.CONTENTS, analyzer, termStr);
       IndexSearcher searcher = new IndexSearcher(reader);
       TotalHitCountCollector totalHitCountCollector = new TotalHitCountCollector();
       searcher.search(query, totalHitCountCollector);
       return Map.ofEntries(
         Map.entry("docFreq", (long) totalHitCountCollector.getTotalHits())
       );
     }
 
     Term t = new Term(IndexArgs.CONTENTS, AnalyzerUtils.analyze(analyzer, termStr).get(0));
     Map<String, Long> termInfo = Map.ofEntries(
       Map.entry("collectionFreq", reader.totalTermFreq(t)),
       Map.entry("docFreq", (long) reader.docFreq(t))
     );
     return termInfo;
   }
 
   /**
    * Returns the document frequency of a term. Simply dispatches to <code>docFreq</code> but wraps the exception so
    * that the caller doesn't need to deal with it; this is potentially dangerous but makes code less verbose.
    *
    * @param reader index reader
    * @param term term
    * @return the document frequency of a term
    */
   public static long getDF(IndexReader reader, String term) {
     try {
       return reader.docFreq(new Term(IndexArgs.CONTENTS, term));
     } catch (IOException e) {
       throw new RuntimeException(e);
     }
   }
 
   /**
    * Returns iterator over all terms in the collection.
    *
    * @param reader index reader
    * @return iterator over IndexTerm
    * @throws IOException if error encountered during access to index
    */
   public static Iterator<IndexTerm> getTerms(IndexReader reader) throws IOException {
     return new Iterator<>() {
       private final TermsEnum curTerm = MultiTerms.getTerms(reader, "contents").iterator();
       private BytesRef bytesRef = null;
 
       @Override
       public boolean hasNext() {
         try {
           // Make sure iterator is positioned.
           if (this.bytesRef == null) {
             return true;
           }
 
           BytesRef originalPos = BytesRef.deepCopyOf(this.bytesRef);
           if (this.curTerm.next() == null) {
             return false;
           } else {
             // Move curTerm back to original position.
             return this.curTerm.seekExact(originalPos);
           }
         } catch (IOException e) {
           return false;
         }
       }
 
       @Override
       public IndexTerm next() {
         try {
           this.bytesRef = this.curTerm.next();
           return new IndexTerm(this.curTerm);
         } catch (IOException e) {
           return null;
         }
       }
     };
   }
 
   /**
    * Returns the postings list for an unanalyzed term. That is, the method analyzes the term before looking up its
    * postings list.
    *
    * @param reader index reader
    * @param term unanalyzed term
    * @return the postings list for an unanalyzed term
    */
   public static List<Posting> getPostingsList(IndexReader reader, String term) {
     return _getPostingsList(reader, AnalyzerUtils.analyze(term).get(0));
   }
 
   /**
    * Returns the postings list for a term.
    *
    * @param reader index reader
    * @param term term
    * @param analyze whether or not the method should analyze the term first
    * @return the postings list for a term
    */
   public static List<Posting> getPostingsList(IndexReader reader, String term, boolean analyze) {
     return _getPostingsList(reader, analyze ? AnalyzerUtils.analyze(term).get(0) : term);
   }
 
   /**
    * Returns the postings list for a term after analysis with a specific analyzer.
    *
    * @param reader index reader
    * @param term term
    * @param analyzer analyzer
    * @return the postings list for an unanalyzed term
    */
   public static List<Posting> getPostingsList(IndexReader reader, String term, Analyzer analyzer) {
     return _getPostingsList(reader, AnalyzerUtils.analyze(analyzer, term).get(0));
   }
 
   // Internal helper: takes the analyzed form in all cases.
   private static List<Posting> _getPostingsList(IndexReader reader, String analyzedTerm) {
     try {
       Term t = new Term(IndexArgs.CONTENTS, analyzedTerm);
       PostingsEnum postingsEnum = MultiTerms.getTermPostingsEnum(reader, IndexArgs.CONTENTS, t.bytes());
 
       List<Posting> postingsList = new ArrayList<>();
       while (postingsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
         postingsList.add(new Posting(postingsEnum));
       }
 
       return postingsList;
     } catch (Exception e) {
       return null;
     }
   }
 
   // These are bindings for Pyserini to work, because Pyjnius can't seem to distinguish overloaded methods.
   // jnius.JavaException: No methods matching your arguments
   public static List<Posting> getPostingsListForUnanalyzedTerm(IndexReader reader, String term) {
     return getPostingsList(reader, term, true);
   }
 
   public static List<Posting> getPostingsListForAnalyzedTerm(IndexReader reader, String term) {
     return getPostingsList(reader, term, false);
   }
 
   public static List<Posting> getPostingsListWithAnalyzer(IndexReader reader, String term, Analyzer analyzer) {
     return getPostingsList(reader, term, analyzer);
   }
 
   /**
    * Returns the document vector for a particular document as a list of tokens contained in the document. Note that this
    * method explicitly returns {@code null} if the document does not exist (as opposed to an empty list), so that the
    * caller is explicitly forced to handle this case.
    *
    * @param reader index reader
    * @param docid collection docid
    * @return the document vector for a particular document as a list of tokens or {@code null} if document does not exist.
    * @throws IOException if error encountered during query
    * @throws NotStoredException if the term vector is not stored or positions are not stored
    */
   public static List<String> getDocumentTokens(IndexReader reader, String docid) throws IOException, NotStoredException {
     int ldocid = convertDocidToLuceneDocid(reader, docid);
     if (ldocid == -1) {
       return null;
     }
     Terms terms = reader.getTermVector(ldocid, IndexArgs.CONTENTS);
     if (terms == null) {
       throw new NotStoredException("Document vector not stored!");
     }
     if (!terms.hasPositions()) {
       throw new NotStoredException("Document vector not stored!");
     }
     TermsEnum te = terms.iterator();
     if (te == null) {
       throw new NotStoredException("Document vector not stored!");
     }
 
     // We need to first find out how long the document vector is so we can allocate an array for it.
     // The temptation is to just call terms.getSumTotalTermFreq(), but we can't - since this value will not include stopwords!
     // The only sure way is to iterate through all the terms once to find the max position.
     // Note that position is zero-based.
     PostingsEnum postingsEnum = null;
     int maxPos = 0;
     while ((te.next()) != null) {
       postingsEnum = te.postings(postingsEnum);
       postingsEnum.nextDoc();
 
       for (int j=0; j<postingsEnum.freq(); j++) {
         int pos = postingsEnum.nextPosition();
         if (pos > maxPos) {
           maxPos = pos;
         }
       }
     }
 
     // We now know how long to make the array.
     String[] tokens = new String[maxPos + 1];
 
     // Go through the terms again, this time to actually build the list of tokens.
     te = reader.getTermVector(ldocid, IndexArgs.CONTENTS).iterator();
     while ((te.next()) != null) {
       postingsEnum = te.postings(postingsEnum);
       postingsEnum.nextDoc();
 
       for (int j=0; j<postingsEnum.freq(); j++) {
         int pos = postingsEnum.nextPosition();
         tokens[pos] = te.term().utf8ToString();
       }
     }
 
     return Arrays.asList(tokens);
   }
 
   /**
    * Returns the document vector for a particular document as a map of terms to term frequencies. Note that this
    * method explicitly returns {@code null} if the document does not exist (as opposed to an empty map), so that the
    * caller is explicitly forced to handle this case.
    *
    * @param reader index reader
    * @param docid collection docid
    * @return the document vector for a particular document as a map of terms to term frequencies or {@code null} if
    * document does not exist.
    * @throws IOException if error encountered during query
    * @throws NotStoredException if the term vector is not stored
    */
   public static Map<String, Long> getDocumentVector(IndexReader reader, String docid) throws IOException, NotStoredException {
     int ldocid = convertDocidToLuceneDocid(reader, docid);
     if (ldocid == -1) {
       return null;
     }
     Terms terms = reader.getTermVector(ldocid, IndexArgs.CONTENTS);
     if (terms == null) {
       throw new NotStoredException("Document vector not stored!");
     }
     TermsEnum te = terms.iterator();
     if (te == null) {
       throw new NotStoredException("Document vector not stored!");
     }
 
     Map<String, Long> docVector = new HashMap<>();
     while ((te.next()) != null) {
       docVector.put(te.term().utf8ToString(), te.totalTermFreq());
     }
 
     return docVector;
   }
 
   /**
    * Returns the term position mapping for a particular document. Note that this method explicitly returns
    * {@code null} if the document does not exist (as opposed to an empty map), so that the caller is explicitly forced
    * to handle this case.
    *
    * @param reader index reader
    * @param docid collection docid
    * @return term position mapping for a particular document or {@code null} if document does not exist.
    * @throws IOException if error encountered during query
    * @throws NotStoredException if the term vector is not stored
    */
   public static Map<String, List<Integer>> getTermPositions(IndexReader reader, String docid) throws IOException, NotStoredException {
     int ldocid = convertDocidToLuceneDocid(reader, docid);
     if (ldocid == -1) {
       return null;
     }
     Terms terms = reader.getTermVector(ldocid, IndexArgs.CONTENTS);
     if (terms == null) {
       throw new NotStoredException("Document vector not stored!");
     }
     TermsEnum termIter = terms.iterator();
     if (termIter == null) {
       throw new NotStoredException("Document vector not stored!");
     }
 
     Map<String, List<Integer>> termPosition = new HashMap<>();
     PostingsEnum positionIter = null;
 
     while ((termIter.next()) != null) {
       List<Integer> positions = new ArrayList<>();
       long termFreq = termIter.totalTermFreq();
       positionIter = termIter.postings(positionIter, PostingsEnum.POSITIONS);
       positionIter.nextDoc();
       for ( int i = 0; i < termFreq; i++ ) {
         positions.add(positionIter.nextPosition());
       }
       termPosition.put(termIter.term().utf8ToString(), positions);
     }
 
     return termPosition;
   }
 
 
/** Returns the Lucene {@link Document} based on a collection docid. */
 public static Document document(IndexReader reader, String docid){}

 

}