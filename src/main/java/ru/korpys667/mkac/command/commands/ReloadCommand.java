/*
 * This file is part of MKAC - https://github.com/korpys667/MKAC
 * Copyright (C) 2026 korpys667, MillyOfficial
 *
 * MKAC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MKAC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.korpys667.mkac.command.commands;

import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import ru.korpys667.mkac.MKAC;
import ru.korpys667.mkac.command.MKCommand;
import ru.korpys667.mkac.sender.Sender;
import ru.korpys667.mkac.utils.Message;
import ru.korpys667.mkac.utils.MessageUtil;

public class ReloadCommand implements MKCommand {

  private final MKAC plugin;

  public ReloadCommand(MKAC plugin) {
    this.plugin = plugin;
  }

  @Override
  public void register(CommandManager<Sender> manager) {
    manager.command(
        manager
            .commandBuilder("mkac")
            .literal("reload")
            .permission("mkac.reload")
            .handler(this::execute));
  }

  private void execute(CommandContext<Sender> context) {
    MessageUtil.sendMessage(context.sender().getNativeSender(), Message.RELOAD_START);
    plugin.reloadPlugin();
    MessageUtil.sendMessage(context.sender().getNativeSender(), Message.RELOAD_SUCCESS);
  }
}
