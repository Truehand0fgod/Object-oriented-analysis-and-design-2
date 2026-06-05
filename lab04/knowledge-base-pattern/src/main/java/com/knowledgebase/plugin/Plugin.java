package com.knowledgebase.plugin;

import javax.swing.Action;
import java.util.List;

/**
 * Любой плагин обязан реализовать этот интерфейс.
 */
public interface Plugin {
    String getName();
    List<Action> getActions(PluginContext context);
}