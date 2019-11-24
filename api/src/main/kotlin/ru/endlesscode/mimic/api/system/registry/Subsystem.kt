/*
 * This file is part of MimicAPI.
 * Copyright (C) 2018 Osip Fatkullin
 * Copyright (C) 2018 EndlessCode Group and contributors
 *
 * MimicAPI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MimicAPI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MimicAPI.  If not, see <http://www.gnu.org/licenses/>.
 */

package ru.endlesscode.mimic.api.system.registry

/**
 * This annotation provides meta-information about subsystem to load it.
 * You must annotate with this all subsystems that you've added.
 */
@Target(AnnotationTarget.CLASS)
@Retention
@MustBeDocumented
annotation class Subsystem(

    /** Priority of subsystem. */
    val priority: SubsystemPriority = SubsystemPriority.NORMAL,

    /**
     * Classes that should exists for subsystem working.
     *
     * Example for SkillAPI:
     * system = ["com.sucy.skill.SkillAPI", "com.sucy.skill.api.player.PlayerData"]
     */
    val classes: Array<String> = []
)