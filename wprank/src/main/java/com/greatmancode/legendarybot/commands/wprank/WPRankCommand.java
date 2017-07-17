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

package com.greatmancode.legendarybot.commands.wprank;

import com.greatmancode.legendarybot.api.commands.PublicCommand;
import com.greatmancode.legendarybot.api.plugin.LegendaryBotPlugin;
import com.greatmancode.legendarybot.api.utils.Utils;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import ro.fortsoft.pf4j.PluginException;
import ro.fortsoft.pf4j.PluginWrapper;

public class WPRankCommand extends LegendaryBotPlugin implements PublicCommand {

    public WPRankCommand(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        String serverName = getBot().getGuildSettings(event.getGuild()).getWowServerName();
        String region = getBot().getGuildSettings(event.getGuild()).getRegionName();
        String guild = getBot().getGuildSettings(event.getGuild()).getGuildName();
        if (serverName == null || region == null || guild == null) {
            event.getChannel().sendMessage("The server name, the region and the guild must be configurated for this command to work!").queue();
            return;
        }

        String result = Utils.doRequest("https://www.wowprogress.com/guild/"+region+"/"+serverName+"/"+guild+"/json_rank");
        System.out.println(result);
        if (result.equals("null")) {
            event.getChannel().sendMessage("Guild not found on WowProgress!").queue();
            return;
        }

        try {
            JSONObject obj = (JSONObject) Utils.jsonParser.parse(result);
            event.getChannel().sendMessage("Guild **" + guild + "** | World: **" + obj.get("world_rank") + "** | Region Rank: **" + obj.get("area_rank") + "** | Realm rank: **" + obj.get("realm_rank") + "**").queue();
        } catch (ParseException e) {
            e.printStackTrace();
        }

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
        return "wprank - Retrive the guild's rank on WowProgress";
    }

    @Override
    public void start() throws PluginException {
        getBot().getCommandHandler().addCommand("wprank", this);
        log.info("Command !wprank loaded!");
    }

    @Override
    public void stop() throws PluginException {
        getBot().getCommandHandler().removeCommand("wprank");
        log.info("Command !wprank unloaded!");
    }
}
