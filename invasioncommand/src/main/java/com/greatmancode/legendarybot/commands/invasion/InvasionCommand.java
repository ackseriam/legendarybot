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
package com.greatmancode.legendarybot.commands.invasion;

import com.greatmancode.legendarybot.api.commands.PublicCommand;
import com.greatmancode.legendarybot.api.commands.ZeroArgsCommand;
import com.greatmancode.legendarybot.api.plugin.LegendaryBotPlugin;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginException;
import ro.fortsoft.pf4j.PluginWrapper;

public class InvasionCommand extends LegendaryBotPlugin implements PublicCommand, ZeroArgsCommand {

    //Start date of the Invasion
    private final static DateTime startDateInvasion = new DateTime(2017,4,14,17,0, DateTimeZone.forID("America/Montreal"));

    private static final Logger log = LoggerFactory.getLogger(InvasionCommand.class);

    public InvasionCommand(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() throws PluginException {
        getBot().getCommandHandler().addCommand("invasion", this);
        log.info("Command !invasion loaded.");

    }

    @Override
    public void stop() throws PluginException {
        getBot().getCommandHandler().removeCommand("invasion");
        log.info("Command !invasion disabled.");
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        int[] timeleft = timeLeftBeforeNextInvasion(new DateTime(DateTimeZone.forID("America/Montreal")));
        if (isInvasionTime(new DateTime(DateTimeZone.forID("America/Montreal")))) {
            event.getChannel().sendMessage("There is currently an invasion active on the Broken Isles! End of the invasion in " + String.format("%02d",timeleft[0]) + ":" + String.format("%02d",timeleft[1]) + " (" + String.format("%02d",timeleft[2])+":" + String.format("%02d",timeleft[3])+")").queue();
        } else {
            event.getChannel().sendMessage("There is no invasions currently active on the Broken Isle. Next invasion in " + String.format("%02d",timeleft[0]) + ":" + String.format("%02d",timeleft[1]) + " (" + String.format("%02d",timeleft[2])+":" + String.format("%02d",timeleft[3])+")").queue();
        }
    }

    @Override
    public String help() {
        return "!invasion - Say if there's currently an invasion running on WoW US!";
    }

    public boolean isInvasionTime(DateTime current) {
        //For the record, the invasion times themselves are NOT random. They are 6 hours on, 12.5 hours off, repeating forever.
        //This gives an invasion happening at every possible hour of the day over a 3 day period.
        DateTime start = new DateTime(startDateInvasion);
        boolean loop = true;
        boolean enabled = true;
        while (loop) {
            if (enabled) {
                start = start.plusHours(6);
                if (current.isBefore(start)) {
                    loop = false;
                } else {
                    enabled = false;
                }
            } else {
                start = start.plusHours(12).plusMinutes(30);
                if (current.isBefore(start)) {
                    loop = false;
                } else {
                    enabled = true;
                }
            }

        }
        return enabled;
    }

    public int[] timeLeftBeforeNextInvasion(DateTime current) {
        //For the record, the invasion times themselves are NOT random. They are 6 hours on, 12.5 hours off, repeating forever.
        //This gives an invasion happening at every possible hour of the day over a 3 day period.
        DateTime start = new DateTime(startDateInvasion);
        boolean loop = true;
        boolean enabled = true;
        Period p = null;
        int hours = 0;
        while (loop) {
            if (enabled) {
                start = start.plusHours(6);
                if (current.isBefore(start)) {
                    loop = false;
                    p = new Period(current, start);
                } else {
                    enabled = false;
                }
            } else {
                start = start.plusHours(12).plusMinutes(30);
                if (current.isBefore(start)) {
                    loop = false;
                    p = new Period(current, start);
                } else {
                    enabled = true;
                }
            }

        }
        return new int[] {p.getHours(), p.getMinutes(),start.getHourOfDay(), start.getMinuteOfHour()};
    }
}