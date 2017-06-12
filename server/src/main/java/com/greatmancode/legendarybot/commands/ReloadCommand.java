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
package com.greatmancode.legendarybot.commands;

import com.greatmancode.legendarybot.api.LegendaryBot;
import com.greatmancode.legendarybot.api.commands.Command;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ro.fortsoft.pf4j.PluginWrapper;

/**
 * Created by greatman on 17-06-10.
 */
public class ReloadCommand implements Command {

    private LegendaryBot bot;
    public ReloadCommand(LegendaryBot bot) {
        this.bot = bot;
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        for (PluginWrapper wrapper : bot.getPluginManager().getPlugins()) {
           bot.getPluginManager().unloadPlugin(wrapper.getPluginId());
        }
        bot.getPluginManager().loadPlugins();
        bot.getPluginManager().startPlugins();
    }

    @Override
    public boolean canExecute(Member member) {
        return member.getUser().getId().equals("95709957629939712");
    }

    @Override
    public int minArgs() {
        return 0;
    }

    @Override
    public int maxArgs() {
        return 0;
    }

    @Override
    public String help() {
        return "!reload - Reload the plugins";
    }
}