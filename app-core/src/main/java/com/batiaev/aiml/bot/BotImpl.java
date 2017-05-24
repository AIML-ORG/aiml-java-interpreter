package com.batiaev.aiml.bot;

import com.batiaev.aiml.chat.ChatContext;
import com.batiaev.aiml.chat.ChatContextStorage;
import com.batiaev.aiml.consts.AimlConst;
import com.batiaev.aiml.core.GraphMaster;
import com.batiaev.aiml.entity.AimlCategory;
import com.batiaev.aiml.entity.AimlMap;
import com.batiaev.aiml.entity.AimlSet;
import com.batiaev.aiml.entity.AimlSubstitution;
import com.batiaev.aiml.loaders.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.batiaev.aiml.channels.ChannelType.CONSOLE;

/**
 * Class representing the AIML bot
 *
 * @author batiaev
 */
@Slf4j
@Component
public class BotImpl implements Bot {

    private GraphMaster brain;
    private BotInfo botInfo;
    private String rootDir;
    private String name;

    @Getter
    private ChatContextStorage chatContextStorage;
    @Setter
    private ChatContext chatContext;

    @Autowired
    public BotImpl(String name, String rootDir, ChatContextStorage chatContextStorage) {
        this.name = name;
        this.rootDir = rootDir;
        this.botInfo = new BotConfiguration(rootDir);
        this.chatContextStorage = chatContextStorage;
        this.chatContext = this.chatContextStorage.getContext(name, CONSOLE);
        brain = new GraphMaster(loadAiml(), loadSets(), loadMaps(), loadSubstitutions());
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrainStats() {
        return brain.getStat();
    }

    public String multisentenceRespond(String request, ChatContext state) {
        String[] sentences = brain.sentenceSplit(request);
        String response = "";
        for (String sentence : sentences)
            response += " " + respond(sentence, state);
        return response.isEmpty() ? AimlConst.error_bot_response : response;
    }

    public String respond(String request, ChatContext state) {
        String pattern = brain.match(request, state.topic(), state.that());
        return brain.respond(pattern, state.topic(), state.that(), state.getPredicates());
    }

    @Override
    public boolean wakeUp() {
        return validate(getRootDir()) && validate(getAimlFolder());
    }

    private List<AimlCategory> loadAiml() {
        AimlLoader loader = new AimlLoader();
        return loader.loadFiles(getAimlFolder());
    }

    private Map<String, AimlSet> loadSets() {

        File sets = new File(getSetsFolder());
        if (!sets.exists()) {
            log.warn("Sets not found!");
            return Collections.emptyMap();
        }
        File[] files = sets.listFiles();
        if (files == null || files.length == 0)
            return Collections.emptyMap();

        FileLoader<AimlSet> loader = new SetLoader();

        final Map<String, AimlSet> data = loader.loadAll(files);
        int count = data.keySet().stream().mapToInt(s -> data.get(s).size()).sum();

        log.info("Loaded {} set records from {} files.", count, files.length);
        return data;
    }

    private Map<String, AimlMap> loadMaps() {

        File maps = new File(getMapsFolder());
        if (!maps.exists()) {
            log.warn("Maps not found!");
            return Collections.emptyMap();
        }
        File[] files = maps.listFiles();
        if (files == null || files.length == 0) return Collections.emptyMap();


        FileLoader<AimlMap> loader = new MapLoader<>();

        final Map<String, AimlMap> data = loader.loadAll(files);
        int count = data.keySet().stream().mapToInt(s -> data.get(s).size()).sum();

        log.info("Loaded " + count + " map records from " + files.length + " files.");
        return data;
    }

    private Map<String, AimlSubstitution> loadSubstitutions() {

        File maps = new File(getSubstitutionsFolder());
        if (!maps.exists()) {
            log.warn("Maps not found!");
            return Collections.emptyMap();
        }
        File[] files = maps.listFiles();
        if (files == null || files.length == 0)
            return Collections.emptyMap();


        FileLoader<AimlSubstitution> loader = new SubstitutionLoader();

        final Map<String, AimlSubstitution> data = loader.loadAll(files);
        int count = data.keySet().stream().mapToInt(s -> data.get(s).size()).sum();

        log.info("Loaded " + count + " substitutions from " + files.length + " files.");
        return data;
    }

    private boolean validate(String folder) {
        if (folder == null || folder.isEmpty())
            return false;
        Path botsFolder = Paths.get(folder);
        if (Files.notExists(botsFolder)) {
            log.warn("BotImpl folder " + folder + " not found!");
            return false;
        }
        return true;
    }

    private String getRootDir() {
        return rootDir;
    }

    private String getAimlFolder() {
        return getRootDir() + "aiml";
    }

    private String getSubstitutionsFolder() {
        return getRootDir() + "substitutions";
    }

    private String getSetsFolder() {
        return getRootDir() + "sets";
    }

    private String getMapsFolder() {
        return getRootDir() + "maps";
    }

    private String getSkillsFolder() {
        return getRootDir() + "skills";
    }

    @Override
    public String getRespond(String phrase) {
        return multisentenceRespond(phrase, chatContext);
    }
}
