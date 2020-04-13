package web.useraction.controller;

import db.operation.StopStem;
import db.operation.InvertedIndex;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.htmlparser.util.ParserException;
import webCrawler.Crawler;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.*;

import org.json.JSONObject;

@WebServlet(urlPatterns = {"/GetUserQueryServlet"})
public class GetUserQueryServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    //Get all the words
    protected Vector<String> returnWords(Crawler crawler) throws ServletException, IOException{
        StopStem stopStem = new StopStem();
        Vector<String> words = new Vector<String>();
        try {
            words = crawler.extractWords();
            for(int i = 0; i< words.size(); i++){
                String current_word = words.get(i).toLowerCase();
                words.set(i,current_word);
                if (current_word.equals("")){
                    System.out.println("empty string");
                    words.remove(i);
                    i--;
                    continue;
                }
                if (stopStem.isStopWord(current_word)) {
                    words.remove(i);
                    i--;
                }else{
                    String new_word = stopStem.stem(current_word);
                    words.set(i,new_word);
                }
            }
        } catch (ParserException e) {
            e.printStackTrace();
        }
        return words;
    }

    //Get all the links
    protected  Vector<String> returnLinks(Crawler crawler) throws ServletException, IOException{
        Vector<String> links = new Vector<String>();
        try {
            links = crawler.extractLinks();
        } catch (ParserException e) {
            e.printStackTrace();
        }
        return links;
    }

    //Get Keyword Frequencies
    protected Map<String, String> returnKeyAndFreq(Vector<String> words){
        Map<String ,String> key_words_freq= new HashMap<String, String>();

        for(int i = 0; i<words.size();i++){
            String current_word = words.get(i);
            if(key_words_freq.get(current_word)==null){
                key_words_freq.put(current_word,"1");
            }else{
                String to_update = key_words_freq.get(current_word);
                Integer count = Integer.parseInt(to_update)+1;
                key_words_freq.put(current_word, String.valueOf(count));
            }
        }
        return key_words_freq;

    }
    protected Map<String, String> returnKeyandPos(Vector<String> words){
        Map<String, String> key_words_pos = new HashMap<String, String>();
        for(int i = 0; i<words.size();i++){
            String current_word = words.get(i);
            if(key_words_pos.get(current_word)==null){
                key_words_pos.put(current_word,Integer.toString(i));
            }else{
                String to_update = key_words_pos.get(current_word) + " " + Integer.toString(i);
                key_words_pos.put(current_word, to_update);
            }
        }
        return key_words_pos;
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        System.out.println("in user crawling controller");

        int numofPages = 30;

        String web_url = request.getParameter("web_url");
        Crawler crawler = new Crawler(web_url);
        Vector<String> words = returnWords(crawler);
        Vector<String> links = returnLinks(crawler);
        Map<String, String> key_words_pos = returnKeyandPos(words);
        Map<String ,String> key_words_freq= returnKeyAndFreq(words);
        Vector<String> words_key = new Vector<String>();
        Vector<String> words_count = new Vector<String>();
        String page_title = crawler.getPageTitle();
        String page_modified_time = crawler.get_last_modified_time();
        Integer page_size = crawler.page_size();
        try{
            //Open RocksDB library
            RocksDB.loadLibrary();
            String path = "/Users/tayingcheng/Desktop/2019-2020Spring/Comp4321/project/comp_4321_project_search_engine/src/main/java/db/data/docs";
            String path2 = "/Users/tayingcheng/Desktop/2019-2020Spring/Comp4321/project/comp_4321_project_search_engine/src/main/java/db/data/words";
            Iterator hmIterator = key_words_freq.entrySet().iterator();
            Iterator posIterator = key_words_pos.entrySet().iterator();
            InvertedIndex wordIndex = new InvertedIndex(path);
            InvertedIndex wordIndexDocs = new InvertedIndex(path2);
            while (hmIterator.hasNext()){
                Map.Entry mapElementFreq = (Map.Entry)hmIterator.next();
                Map.Entry mapElementPos = (Map.Entry)posIterator.next();
                String curWord = (String)mapElementFreq.getKey();
                String curValue = (String)mapElementFreq.getValue();
                String curPostList = (String)mapElementPos.getValue();
                words_key.addElement(curWord);
                words_count.addElement(curValue);
                wordIndex.addEntry(curWord, 0, Integer.parseInt(curValue), curPostList);
//                wordIndex.printAll();
            }
            wordIndexDocs.addEntryDocs("0", page_title, page_modified_time, web_url, page_size, links.toString(), words_key.toString(), words_count.toString());
            //wordIndexDocs.printAll();
            for(int i = 1; i <= 30; i ++){
                Crawler newCrawler = new Crawler(links.get(i));
                String curPageTitle = newCrawler.getPageTitle();
                String curPageModified_time = newCrawler.get_last_modified_time();
                int curPageSize = newCrawler.page_size();
                Vector<String> curWords = returnWords(newCrawler);
                Vector<String> curLinks = returnLinks(newCrawler);
                Map<String, String> curkey_words_pos = returnKeyandPos(curWords);
                Map<String ,String> curkey_words_freq= returnKeyAndFreq(curWords);
                Vector<String> curwords_key = new Vector<String>();
                Vector<String> curwords_count = new Vector<String>();
                Iterator curhmIterator = key_words_freq.entrySet().iterator();
                Iterator curposIterator = key_words_pos.entrySet().iterator();
                while(curhmIterator.hasNext()){
                    Map.Entry mapElementFreq = (Map.Entry)curhmIterator.next();
                    Map.Entry mapElementPos = (Map.Entry)curposIterator.next();
                    String curWord = (String)mapElementFreq.getKey();
                    String curValue = (String)mapElementFreq.getValue();
                    String curPostList = (String)mapElementPos.getValue();
                    curwords_key.addElement(curWord);
                    curwords_count.addElement(curValue);
                    wordIndex.addEntry(curWord, i, Integer.parseInt(curValue), curPostList);
                }
                wordIndexDocs.addEntryDocs(String.valueOf(i), curPageTitle, curPageModified_time,links.get(i), curPageSize, curLinks.toString(), curwords_key.toString(), curwords_count.toString());

            }
            wordIndexDocs.printAll();
        }
        catch(RocksDBException e){
            System.out.println("pull up! stooooooopid");
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");

        /* construct your json */
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("words_key", words_key);
        jsonResponse.put("words_count", words_count);
        jsonResponse.put("links_list", links);
        jsonResponse.put("page_url", web_url);
        jsonResponse.put("page_title", page_title );
        jsonResponse.put("page_size",page_size);
        jsonResponse.put("page_last_modified_time", page_modified_time);
        /* send to the client the JSON string */
        response.getWriter().write(jsonResponse.toString());
    }
}