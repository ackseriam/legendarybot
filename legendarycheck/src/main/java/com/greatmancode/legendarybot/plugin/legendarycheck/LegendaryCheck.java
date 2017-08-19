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
package com.greatmancode.legendarybot.plugin.legendarycheck;

import com.greatmancode.legendarybot.api.LegendaryBot;
import com.greatmancode.legendarybot.api.utils.BattleNetAPIInterceptor;
import net.dv8tion.jda.core.entities.Guild;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

public class LegendaryCheck {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private long[] itemIDIgnore = {147451,151462};
    public LegendaryCheck(LegendaryBot bot, Guild guild, LegendaryCheckPlugin plugin) {
        final Runnable checkNews = () -> {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new BattleNetAPIInterceptor(bot))
                    .build();
            try {
                String serverName = plugin.getBot().getGuildSettings(guild).getWowServerName();
                String regionName = plugin.getBot().getGuildSettings(guild).getRegionName();
                String guildName = plugin.getBot().getGuildSettings(guild).getGuildName();
                String channelName = plugin.getBot().getGuildSettings(guild).getSetting(LegendaryCheckPlugin.SETTING_NAME);
                if (regionName == null || serverName == null || guildName == null || channelName == null) {
                    return;
                }

                HttpUrl url = new HttpUrl.Builder().scheme("https")
                        .host(regionName + ".api.battle.net")
                        .addPathSegments("wow/guild/" + serverName + "/" + guildName)
                        .addQueryParameter("fields", "members")
                        .build();
                Request webRequest = new Request.Builder().url(url).build();

                String request = client.newCall(webRequest).execute().body().string();
                if (request == null) {
                    return;
                } else if (request.contains("nok")) {
                    return;
                }
                try {
                    System.out.println("Starting Legendary check for server " + guild.getName());
                    JSONObject object = (JSONObject) new JSONParser().parse(request);
                    JSONArray membersArray = (JSONArray) object.get("members");
                    for (Object memberObject : membersArray) {
                        JSONObject member = (JSONObject) ((JSONObject) memberObject).get("character");
                        String name = (String) member.get("name");
                        String realm = (String) member.get("realm");
                        if (realm == null) {
                            realm = (String) member.get("guildRealm");
                        }
                        long level = (Long) member.get("level");
                        if (level != 110) {
                            continue;
                        }
                        url = new HttpUrl.Builder().scheme("https")
                                .host(regionName + ".api.battle.net")
                                .addPathSegments("/wow/character/" + realm + "/" + name)
                                .addQueryParameter("fields", "feed")
                                .build();
                        webRequest = new Request.Builder().url(url).build();
                        String memberFeedRequest = client.newCall(webRequest).execute().body().string();
                        if (memberFeedRequest == null) {
                            continue;
                        } else if (!memberFeedRequest.contains("lastModified")) {
                            System.out.println("Guild " + guild.getName() + "("+guild.getId()+") Member " + name + " with realm " + realm + " not found for WoW guild " + guildName + "-" + regionName);
                            continue;
                        }
                        JSONObject memberJson = (JSONObject) new JSONParser().parse(memberFeedRequest);

                        long memberLastModified = (Long) memberJson.get("lastModified");
                        long dbLastModified = plugin.getPlayerInventoryDate(regionName, serverName, name);
                        if (memberLastModified <= dbLastModified) {
                            continue;
                        }
                        plugin.setPlayerInventoryDate(regionName, serverName, name, memberLastModified);
                        //We check the items
                        JSONArray feedArray = (JSONArray) memberJson.get("feed");
                        for (Object feedObject : feedArray) {
                            JSONObject feed = (JSONObject) feedObject;
                            if (feed.get("type").equals("LOOT")) {
                                long itemID = (Long) feed.get("itemId");
                                long timestamp = (Long) feed.get("timestamp");
                                if (timestamp <= dbLastModified) {
                                    continue;
                                }
                                if (LongStream.of(itemIDIgnore).anyMatch(x -> x == itemID)) {
                                    continue;
                                }
                                url = new HttpUrl.Builder().scheme("https")
                                        .host(regionName + ".api.battle.net")
                                        .addPathSegments("/wow/item/" + itemID)
                                        .build();
                                webRequest = new Request.Builder().url(url).build();
                                String itemRequest = client.newCall(webRequest).execute().body().string();
                                if (itemRequest == null) {
                                    continue;
                                }
                                JSONObject itemObject = null;
                                try {
                                    itemObject = (JSONObject) new JSONParser().parse(itemRequest);
                                } catch (ParseException e) {
                                    bot.getStacktraceHandler().sendStacktrace(e, "guildId:" + guild.getId(), "region:" + regionName, "wowGuild:" + guildName, "serverName:" + serverName, "channelName:" + channelName, "itemRequest:" + itemRequest);
                                    continue;
                                }

                                long quality = (Long) itemObject.get("quality");
                                if (quality == 5) {
                                    System.out.println(name + " just looted a legendary");
                                    //We got a legendary!
                                    guild.getTextChannelsByName(channelName, true).get(0).sendMessage(name + " just looted the legendary " + itemObject.get("name") + "! :tada:  http://www.wowhead.com/item=" + itemID).queue();
                                }
                            }
                            if (Thread.interrupted()) {
                                return;
                            }
                        }
                        if (Thread.interrupted()) {
                            return;
                        }
                    }
                    System.out.println("Went through Legendary check for server " + guild.getName());
                } catch (ParseException e) {
                    e.printStackTrace();
                    bot.getStacktraceHandler().sendStacktrace(e, "guildId:" + guild.getId(), "region:" + regionName, "wowGuild:" + guildName, "serverName:" + serverName, "channelName:" + channelName);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    bot.getStacktraceHandler().sendStacktrace(e, "guildId:" + guild.getId(), "region:" + regionName, "wowGuild:" + guildName, "serverName:" + serverName, "channelName:" + channelName);
                }
            } catch (Throwable e) {
                e.printStackTrace();

                System.out.println("Crashed for guild " + guild.getName() + ":" + guild.getId());
                bot.getStacktraceHandler().sendStacktrace(e, "guildId:" + guild.getId());
            }
        };
        scheduler.scheduleAtFixedRate(checkNews, 0,10, TimeUnit.MINUTES);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
