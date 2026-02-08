package de.pianoman911.playerculling.meme.generator.config;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import de.pianoman911.playerculling.meme.generator.AbstractGenerator;

import java.util.HashMap;
import java.util.Map;

public class GeneratorConfig {

    private Map<Class<?>, String> fieldBlacklist = new HashMap<>();
    private Table<Class<?>, String, AbstractGenerator<?, ?, ?>> generators = HashBasedTable.create();


    public Map<Class<?>, String> getFieldBlacklist() {
        return this.fieldBlacklist;
    }

    public Table<Class<?>, String, AbstractGenerator<?, ?, ?>> getGenerators() {
        return this.generators;
    }

}
