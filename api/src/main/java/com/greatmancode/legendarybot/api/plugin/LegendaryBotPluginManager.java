/*
 * MIT License
 *
 * Copyright (c) Copyright (c) 2017-2017, Greatmancode
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.greatmancode.legendarybot.api.plugin;

import com.greatmancode.legendarybot.api.LegendaryBot;
import ro.fortsoft.pf4j.DefaultPluginManager;
import ro.fortsoft.pf4j.PluginClasspath;

public class LegendaryBotPluginManager extends DefaultPluginManager {

    private final LegendaryBot bot;
    public LegendaryBotPluginManager(LegendaryBot bot) {
        super();
        this.bot = bot;
    }

    public LegendaryBot getBot() {
        return bot;
    }

    protected PluginClasspath createPluginClasspath() {
        return new LegendaryBotClasspath();
    }
}