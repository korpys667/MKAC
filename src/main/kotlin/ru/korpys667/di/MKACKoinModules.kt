/*
 * This file is part of MKAC - https://github.com/korpys667/MKAC
 * Copyright (C) 2026 korpys667
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
package ru.korpys667.di

import java.util.logging.Logger
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.command.CommandSender
import org.incendo.cloud.SenderMapper
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.korpys667.MKAC
import ru.korpys667.MKACCore
import ru.korpys667.ai.AiResponseParser
import ru.korpys667.ai.AiService
import ru.korpys667.ai.DefaultAiService
import ru.korpys667.ai.JacksonAiResponseParser
import ru.korpys667.alert.AlertManager
import ru.korpys667.api.MKACApi
import ru.korpys667.api.event.MKACEventBus
import ru.korpys667.api.event.internal.MKACEventBusImpl
import ru.korpys667.api.internal.AiApiImpl
import ru.korpys667.api.internal.CheckApiImpl
import ru.korpys667.api.internal.MKACApiImpl
import ru.korpys667.api.internal.MonitorApiImpl
import ru.korpys667.api.internal.PunishmentApiImpl
import ru.korpys667.api.service.AiApi
import ru.korpys667.api.service.CheckApi
import ru.korpys667.api.service.MonitorApi
import ru.korpys667.api.service.PunishmentApi
import ru.korpys667.checks.CheckFactory
import ru.korpys667.checks.CheckManager
import ru.korpys667.checks.impl.ai.AiCheck
import ru.korpys667.checks.impl.ai.AiSnapshotStore
import ru.korpys667.checks.impl.ai.PersistentBufferService
import ru.korpys667.checks.impl.combat.AimProcessor
import ru.korpys667.checks.impl.misc.ClientBrand
import ru.korpys667.command.CommandManager
import ru.korpys667.command.CommandRegister
import ru.korpys667.command.MKACCommand
import ru.korpys667.command.commands.admin.AlertsCommand
import ru.korpys667.command.commands.admin.BrandsCommand
import ru.korpys667.command.commands.admin.BufferCommand
import ru.korpys667.command.commands.admin.CollectCommand
import ru.korpys667.command.commands.admin.ExemptCommand
import ru.korpys667.command.commands.admin.MitigationsCommand
import ru.korpys667.command.commands.admin.PunishCommand
import ru.korpys667.command.commands.admin.ReloadCommand
import ru.korpys667.command.commands.admin.SuspiciousCommand
import ru.korpys667.command.commands.info.HelpCommand
import ru.korpys667.command.commands.info.HistoryCommand
import ru.korpys667.command.commands.info.LogsCommand
import ru.korpys667.command.commands.info.MonitorCommand
import ru.korpys667.command.commands.info.MonitorInfoCommand
import ru.korpys667.command.commands.info.MonitorOutputSelector
import ru.korpys667.command.commands.info.MonitorSettingsCommand
import ru.korpys667.command.commands.info.ProfileCommand
import ru.korpys667.command.commands.info.StatsCommand
import ru.korpys667.command.commands.info.ViewCommand
import ru.korpys667.command.handler.MKACCommandFailureHandler
import ru.korpys667.config.ConfigManager
import ru.korpys667.config.LocaleManager
import ru.korpys667.coroutines.MKACCoroutines
import ru.korpys667.damage.DamageProcessor
import ru.korpys667.data.CollectManager
import ru.korpys667.database.DatabaseManager
import ru.korpys667.debug.DebugManager
import ru.korpys667.integration.WorldGuardManager
import ru.korpys667.mitigation.HitStamps
import ru.korpys667.mitigation.MitigationDamageProcessor
import ru.korpys667.mitigation.MitigationLogStore
import ru.korpys667.mitigation.MitigationRuntime
import ru.korpys667.mitigation.MitigationScoreStore
import ru.korpys667.mitigation.MitigationScorer
import ru.korpys667.mitigation.MitigationSkip
import ru.korpys667.mitigation.RuleEngine
import ru.korpys667.monitor.MonitorServices
import ru.korpys667.monitor.core.ComponentCache
import ru.korpys667.monitor.core.MonitorSampler
import ru.korpys667.monitor.core.MonitorSettingsService
import ru.korpys667.monitor.core.ScoreboardPacketBridge
import ru.korpys667.monitor.core.ScoreboardSlotObserver
import ru.korpys667.monitor.core.ScoreboardSlotRegistry
import ru.korpys667.monitor.hud.MonitorFrameBuilder
import ru.korpys667.monitor.hud.MonitorHudService
import ru.korpys667.monitor.hud.MonitorLiveChatListener
import ru.korpys667.monitor.hud.MonitorOutput
import ru.korpys667.monitor.hud.MonitorOutputFailureSink
import ru.korpys667.monitor.hud.MonitorOutputGuard
import ru.korpys667.monitor.hud.MonitorOutputRegistry
import ru.korpys667.monitor.hud.MonitorRuntime
import ru.korpys667.monitor.hud.MonitorTargetIndex
import ru.korpys667.monitor.hud.MonitorTargetsService
import ru.korpys667.monitor.hud.output.ActionBarOutput
import ru.korpys667.monitor.hud.output.BossBarOutput
import ru.korpys667.monitor.hud.output.ChatOutput
import ru.korpys667.monitor.hud.output.ChatSink
import ru.korpys667.monitor.hud.output.SidebarOutput
import ru.korpys667.monitor.hud.output.TabListOutput
import ru.korpys667.monitor.view.MonitorViewService
import ru.korpys667.packet.PacketListener
import ru.korpys667.platform.scheduler.PlatformScheduler
import ru.korpys667.platform.scheduler.PlatformSchedulerFactory
import ru.korpys667.player.ExemptManager
import ru.korpys667.player.PlayerDataManager
import ru.korpys667.punishment.PunishmentManager
import ru.korpys667.redis.CrossServerAlertService
import ru.korpys667.redis.CrossServerServices
import ru.korpys667.redis.CrossServerSuspiciousService
import ru.korpys667.redis.RedisManager
import ru.korpys667.region.RegionProvider
import ru.korpys667.scheduler.SchedulerService
import ru.korpys667.sender.Sender
import ru.korpys667.sender.SenderFactory
import ru.korpys667.server.AIServerProvider
import ru.korpys667.telemetry.DetectionReporter
import ru.korpys667.telemetry.TelemetryService
import ru.korpys667.utils.MessageUtil

fun mkacModules(plugin: MKAC) =
  listOf(
    coreModule(plugin),
    monitorModule(),
    aiModule(),
    apiModule(),
    commandModule(),
    checkModule(),
  )

@Suppress("LongMethod")
private fun coreModule(plugin: MKAC) = module {
  single { plugin }
  single { BukkitAudiences.create(plugin) }
  single<Logger> { plugin.logger }
  single<PlatformScheduler> { PlatformSchedulerFactory.create() }
  single<MKACEventBus> { MKACEventBusImpl() }

  singleOf(::SchedulerService)
  singleOf(::MKACCoroutines)
  singleOf(::ConfigManager)
  singleOf(::TelemetryService)
  singleOf(::DetectionReporter)
  singleOf(::LocaleManager)
  singleOf(::DatabaseManager)
  singleOf(::DebugManager)
  singleOf(::AIServerProvider)
  singleOf(::AlertManager)
  singleOf(::RedisManager)
  singleOf(::CrossServerAlertService)
  singleOf(::CrossServerSuspiciousService)
  single { ComponentCache() }
  singleOf(::MonitorSampler)
  single { ScoreboardPacketBridge(get()) }
  singleOf(::ScoreboardSlotRegistry)
  singleOf(::ScoreboardSlotObserver)
  singleOf(::MonitorSettingsService)
  singleOf(::MonitorViewService)
  singleOf(::ExemptManager)
  singleOf(::CollectManager)
  singleOf(::PersistentBufferService)
  singleOf(::WorldGuardManager)
  single<RegionProvider> { get<WorldGuardManager>() }

  single { { get<ConfigManager>().mitigationSettings } }
  single { HitStamps() }
  single { MitigationScorer(get(), get()) }
  single { MitigationSkip(get(), get(), get()) }
  single { RuleEngine(get(), System::currentTimeMillis) }
  single { MitigationDamageProcessor() }
  single<DamageProcessor> { get<MitigationDamageProcessor>() }
  single { MitigationScoreStore(get(), get(), get()) }
  single { MitigationLogStore(get(), get(), get()) }
  single { AiSnapshotStore(get()) }
  single {
    MitigationRuntime(
      plugin = get(),
      playerDataManager = get(),
      configManager = get(),
      alertManager = get(),
      skip = get(),
      engine = get(),
      damageProcessor = get(),
      stamps = get(),
      debugManager = get(),
      scheduler = get(),
      logStore = get(),
      settings = get(),
    )
  }

  singleOf(::SenderFactory).bind<SenderMapper<CommandSender, Sender>>()

  singleOf(::MKACCommandFailureHandler)

  singleOf(::PlayerDataManager)
  singleOf(::PacketListener)

  singleOf(::CrossServerServices)
  singleOf(::MKACCore)
}

private fun monitorModule() = module {
  singleOf(::MonitorServices)
  single { MonitorFrameBuilder(get<ConfigManager>().labelCatalog) }
  singleOf(::MonitorTargetIndex)
  singleOf(::MonitorTargetsService)
  singleOf(::MonitorOutputSelector)
  single<MonitorOutputFailureSink> {
    val scope = this
    MonitorOutputFailureSink { viewerId, kind, phase, error ->
      scope.get<MonitorHudService>().onOutputFailed(viewerId, kind, phase, error)
    }
  }
  single<ChatSink> {
    ChatSink { viewer, raw -> MessageUtil.sendMessage(viewer, MessageUtil.format(raw)) }
  }
  single<MonitorOutput>(named("actionbar")) {
    MonitorOutputGuard(ActionBarOutput(get(), get()), get(), get())
  }
  single<MonitorOutput>(named("bossbar")) {
    MonitorOutputGuard(BossBarOutput(get(), get()), get(), get())
  }
  single<MonitorOutput>(named("sidebar")) {
    MonitorOutputGuard(SidebarOutput(get(), get()), get(), get())
  }
  single { ChatOutput(get()) }
  single<MonitorOutput>(named("chat")) { MonitorOutputGuard(get<ChatOutput>(), get(), get()) }
  single<MonitorOutput>(named("tablist")) {
    MonitorOutputGuard(TabListOutput(get(), get()), get(), get())
  }
  single { MonitorOutputRegistry(getAll()) }
  singleOf(::MonitorHudService)
  singleOf(::MonitorLiveChatListener)
  singleOf(::MonitorRuntime)
}

private fun aiModule() = module {
  singleOf(::JacksonAiResponseParser).bind<AiResponseParser>()
  singleOf(::DefaultAiService).bind<AiService>()
}

private fun apiModule() = module {
  singleOf(::AiApiImpl).bind<AiApi>()
  singleOf(::CheckApiImpl).bind<CheckApi>()
  singleOf(::MonitorApiImpl).bind<MonitorApi>()
  singleOf(::PunishmentApiImpl).bind<PunishmentApi>()
  singleOf(::MKACApiImpl).bind<MKACApi>()
}

private fun commandModule() = module {
  includes(adminCommandsModule(), infoCommandsModule())

  single { CommandRegister(getAll(), get()) }
  singleOf(::CommandManager)
}

private fun adminCommandsModule() = module {
  singleOf(::AlertsCommand).bind<MKACCommand>()
  singleOf(::BrandsCommand).bind<MKACCommand>()
  singleOf(::CollectCommand).bind<MKACCommand>()
  singleOf(::MitigationsCommand).bind<MKACCommand>()
  singleOf(::ExemptCommand).bind<MKACCommand>()
  singleOf(::PunishCommand).bind<MKACCommand>()
  singleOf(::BufferCommand).bind<MKACCommand>()
  singleOf(::ReloadCommand).bind<MKACCommand>()
  singleOf(::SuspiciousCommand).bind<MKACCommand>()
}

private fun infoCommandsModule() = module {
  singleOf(::HelpCommand).bind<MKACCommand>()
  singleOf(::HistoryCommand).bind<MKACCommand>()
  singleOf(::LogsCommand).bind<MKACCommand>()
  singleOf(::MonitorCommand).bind<MKACCommand>()
  singleOf(::MonitorSettingsCommand).bind<MKACCommand>()
  singleOf(::MonitorInfoCommand).bind<MKACCommand>()
  singleOf(::ProfileCommand).bind<MKACCommand>()
  singleOf(::StatsCommand).bind<MKACCommand>()
  singleOf(::ViewCommand).bind<MKACCommand>()
}

private fun checkModule() = module {
  single<CheckFactory>(named("aim")) { CheckFactory { player -> AimProcessor(player) } }
  single<CheckFactory>(named("ai")) {
    CheckFactory { player ->
      AiCheck(player, get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
  }
  single<CheckFactory>(named("brand")) {
    CheckFactory { player -> ClientBrand(player, get(), get()) }
  }

  single<Set<CheckFactory>> { getAll<CheckFactory>().toSet() }

  single<CheckManager.Factory> { CheckManager.Factory { player -> CheckManager(player, get()) } }
  single<PunishmentManager.Factory> {
    PunishmentManager.Factory { player ->
      PunishmentManager(player, get(), get(), get(), get(), get(), get(), get(), get())
    }
  }
}
