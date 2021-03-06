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
import net.dv8tion.jda.core.entities.TextChannel;
import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

/**
 * The LegendaryCheck handler.
 */
public class LegendaryCheck {

    /**
     * The scheduler for the checks.
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * The LegendaryBot instance.
     */
    private LegendaryBot bot;

    /**
     * ItemIDs to ignore, those are not Legendaries that we want to announce.
     */
    private long[] itemIDIgnore = {147451,151462, 152626, 154880};

    /**
     * The Logger
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Instantiate a LegendaryCheck handler
     * @param bot The bot instance.
     * @param guild The guild to check legendaries in.
     * @param plugin The Legendary Check plugin instance.
     * @param initialDelay The initial delay before starting the check.
     */
    public LegendaryCheck(LegendaryBot bot, Guild guild, LegendaryCheckPlugin plugin, int initialDelay) {
        this.bot = bot;
        final Runnable checkNews = () -> {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new BattleNetAPIInterceptor(bot))
                    .connectionPool(new ConnectionPool(300, 1, TimeUnit.SECONDS))
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

                okhttp3.Response response = client.newCall(webRequest).execute();
                String request = response.body().string();
                response.close();
                if (request == null) {
                    return;
                } else if (request.contains("nok")) {
                    return;
                }
                try {
                    log.info("Starting Legendary check for server " + guild.getName());
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
                        response = client.newCall(webRequest).execute();
                        String memberFeedRequest = response.body().string();
                        response.close();
                        if (memberFeedRequest == null) {
                            continue;
                        } else if (!memberFeedRequest.contains("lastModified")) {
                            log.warn("Guild " + guild.getName() + "("+guild.getId()+") Member " + name + " with realm " + realm + " not found for WoW guild " + guildName + "-" + regionName);
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
                                if (isItemLegendary(regionName, itemID)) {
                                    log.info(name + " just looted a legendary");
                                    //We got a legendary!
                                    List<TextChannel> channelList = guild.getTextChannelsByName(channelName, true);
                                    if (channelList.isEmpty()) {
                                        plugin.destroyLegendaryCheck(guild);
                                        log.warn("Guild " + guild + "("+guild.getId()+") have a invalid channel name " + channelName + ". Removing legendary check.");
                                    } else {
                                        channelList.get(0).sendMessage(bot.getTranslateManager().translate(guild,"legendarycheck.looted",name,getItemName(regionName, itemID)) + " http://www.wowhead.com/item=" + itemID).queue();
                                    }

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
                    log.info("Went through Legendary check for server " + guild.getName());
                } catch (ParseException | NullPointerException e) {
                    e.printStackTrace();
                    bot.getStacktraceHandler().sendStacktrace(e, "guildId:" + guild.getId(), "region:" + regionName, "wowGuild:" + guildName, "serverName:" + serverName, "channelName:" + channelName);
                }
            } catch (Throwable e) {
                e.printStackTrace();

                log.error("Crashed for guild " + guild.getName() + ":" + guild.getId());
                bot.getStacktraceHandler().sendStacktrace(e, "guildId:" + guild.getId());
            }
        };
        scheduler.scheduleAtFixedRate(checkNews, initialDelay,1200, TimeUnit.SECONDS);
    }

    /**
     * Stop the legendary checker. Stops the scheduler.
     */
    public void shutdown() {
        scheduler.shutdownNow();
    }

    /**
     * Check if an item is a legendary. Checks in the ES cache if we have the item. If not, we retrieve the information from Battle.Net API and cache it.
     * @param regionName The region to check the item in.
     * @param itemID The Item ID to check.
     * @return True if the item is a legendary. Else false.
     */
    public boolean isItemLegendary(String regionName, long itemID) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new BattleNetAPIInterceptor(bot))
                .build();
        try {
            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("q", "id:" + itemID);
            Response response = bot.getElasticSearch().performRequest("GET", "/wow/item/_search", paramMap);

            JSONParser jsonParser = new JSONParser();
            try {
                JSONObject obj = (JSONObject) jsonParser.parse(EntityUtils.toString(response.getEntity()));
                JSONObject hits = (JSONObject) obj.get("hits");
                if ((long)hits.get("total") == 0) {
                    HttpUrl url = new HttpUrl.Builder().scheme("https")
                            .host(regionName + ".api.battle.net")
                            .addPathSegments("/wow/item/" + itemID)
                            .build();
                    Request webRequest = new Request.Builder().url(url).build();
                    okhttp3.Response responseBattleNet = client.newCall(webRequest).execute();
                    String itemRequest = responseBattleNet.body().string();
                    responseBattleNet.close();
                    if (itemRequest == null) {
                        return false;
                    }
                    JSONObject itemObject;
                    try {
                        itemObject = (JSONObject) new JSONParser().parse(itemRequest);
                    } catch (ParseException e) {
                        bot.getStacktraceHandler().sendStacktrace(e, "itemID:" + itemID, "regionName:" + regionName, "itemRequest:" + itemRequest);
                        return false;
                    }
                    if (itemObject.containsKey("reason")) {
                        return false;
                    }

                    HttpEntity entity = new NStringEntity(itemObject.toJSONString(), ContentType.APPLICATION_JSON);
                    Response indexResponse = bot.getElasticSearch().performRequest("POST", "/wow/item/",Collections.emptyMap(), entity);
                    long quality = (Long) itemObject.get("quality");
                    return quality == 5;
                }

                JSONArray hit = (JSONArray) ((JSONObject)obj.get("hits")).get("hits");
                JSONObject firstItem = (JSONObject) hit.get(0);
                JSONObject source = (JSONObject) firstItem.get("_source");
                return (long) source.get("quality") == 5;
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            bot.getStacktraceHandler().sendStacktrace(e);
            return false;
        }
    }

    /**
     * Retrieve the item name. Checks in the ES cache if we have the item. If not, we retrieve the information from Battle.Net API and cache it.
     * @param regionName The region to check in.
     * @param itemID The item ID.
     * @return The name of the item. Else null if not found.
     */
    public String getItemName(String regionName, long itemID) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new BattleNetAPIInterceptor(bot))
                .build();
        try {
            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("q", "id:" + itemID);
            Response response = bot.getElasticSearch().performRequest("GET", "/wow/item/_search", paramMap);

            JSONParser jsonParser = new JSONParser();
            try {
                JSONObject obj = (JSONObject) jsonParser.parse(EntityUtils.toString(response.getEntity()));
                JSONObject hits = (JSONObject) obj.get("hits");
                if ((long)hits.get("total") == 0) {
                    HttpUrl url = new HttpUrl.Builder().scheme("https")
                            .host(regionName + ".api.battle.net")
                            .addPathSegments("/wow/item/" + itemID)
                            .build();
                    Request webRequest = new Request.Builder().url(url).build();
                    okhttp3.Response responseBattleNet = client.newCall(webRequest).execute();
                    String itemRequest = responseBattleNet.body().string();
                    responseBattleNet.close();
                    if (itemRequest == null) {
                        return null;
                    }
                    JSONObject itemObject;
                    try {
                        itemObject = (JSONObject) new JSONParser().parse(itemRequest);
                    } catch (ParseException e) {
                        bot.getStacktraceHandler().sendStacktrace(e, "itemID:" + itemID, "regionName:" + regionName, "itemRequest:" + itemRequest);
                        return null;
                    }
                    return (String) itemObject.get("name");
                }
                JSONArray hit = (JSONArray) ((JSONObject)obj.get("hits")).get("hits");
                JSONObject firstItem = (JSONObject) hit.get(0);
                JSONObject source = (JSONObject) firstItem.get("_source");
                return (String) source.get("name");
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            bot.getStacktraceHandler().sendStacktrace(e);
            return null;
        }
    }
}
