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

package ru.endlesscode.mimic.api.system.registry;

import org.jetbrains.annotations.NotNull;
import ru.endlesscode.mimic.api.system.PlayerSystem;
import ru.endlesscode.mimic.api.system.SystemFactory;

/**
 * This is class responsible for accounting all system hooks.
 *
 * @apiNote
 * Systems - it is classes that directly extends PlayerSystem. Subsystems - it
 * is concrete implementations of systems.
 *
 * @implNote
 * Recommended to use a singleton pattern when implement this class.
 *
 * @author Osip Fatkullin
 * @since 0.1
 */
public abstract class SystemRegistry {
    /**
     * Registers hook of subsystem that given as class. Use registering
     * with instance instead, if you can. Because this method is a slower.
     *
     * @param <SubsystemT>   Subsystem type
     * @param subsystemClass Class of the subsystem
     * @throws SystemNotRegisteredException If registering failed
     * @throws SystemNotNeededException     If registering not needed
     */
    public <SubsystemT extends PlayerSystem> void registerSubsystem(
            @NotNull Class<SubsystemT> subsystemClass)
            throws SystemNotRegisteredException, SystemNotNeededException {
        this.registerSubsystem(subsystemClass, null);
    }

    /**
     * Adds hook of subsystem if subsystem is can be added.
     *
     * @implNote
     * If registering fails, will be thrown exception.
     *
     * @param <SubsystemT>   Subsystem type
     * @param subsystemClass Class of the subsystem
     * @param systemFactory  Subsystem factory (can be {@code null})
     * @throws SystemNotRegisteredException If registering failed
     * @throws SystemNotNeededException     If registering not needed
     */
    public <SubsystemT extends PlayerSystem> void registerSubsystem(
            @NotNull Class<SubsystemT> subsystemClass,
            SystemFactory<? super SubsystemT> systemFactory)
            throws SystemNotRegisteredException, SystemNotNeededException {
        try {
            this.tryToRegisterSubsystem(subsystemClass, systemFactory);
        } catch (IllegalArgumentException | ClassCastException e) {
            throw new SystemNotRegisteredException("System didn't registered.", e);
        }
    }

    /**
     * Tries to register given factory. If hook failed throws exception.
     *
     * @implNote
     * If {@code givenFactory} is {@code null}, it gets from class. You can override
     * {@link #getSubsystemFactory(Class)} to change factory getting algorithm.
     *
     * @param <SystemT>      System type
     * @param subsystemClass Class of the subsystem
     * @param givenFactory   Instance of the system factory (can be {@code null})
     * @throws SystemNotNeededException If some requirements aren't met
     */
    protected <SystemT extends PlayerSystem> void tryToRegisterSubsystem(
            @NotNull Class<? extends SystemT> subsystemClass,
            SystemFactory<SystemT> givenFactory)
            throws SystemNotNeededException {
        MetadataAdapter meta = MetadataAdapter.getNotNullMeta(subsystemClass);
        if (!meta.requiredClassesExists()) {
            throw new SystemNotNeededException(
                    String.format("Required classes for '%s' not found.", subsystemClass.getSimpleName()));
        }

        SystemFactory<SystemT> factory = givenFactory == null ? this.getSubsystemFactory(subsystemClass) : givenFactory;
        //noinspection unchecked
        Class<SystemFactory<SystemT>> factoryClass = (Class<SystemFactory<SystemT>>) factory.getClass();

        this.registerSystem(factoryClass, factory, meta.getPriority());
    }

    /**
     * Gets and returns factory from given class.
     *
     * @implSpec
     * Should not return {@code null}. Throw {@code IllegalArgumentException} instead.
     * In every subsystem needed to create factory
     *
     * @param <SystemT>      System type
     * @param subsystemClass Class of the subsystem
     * @return Factory of system
     * @throws IllegalArgumentException If factory not found
     */
    protected <SystemT extends PlayerSystem> @NotNull SystemFactory<SystemT> getSubsystemFactory(
            @NotNull Class<? extends SystemT> subsystemClass) {
        try {
            //noinspection unchecked
            return (SystemFactory<SystemT>) subsystemClass.getField("FACTORY").get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Factory not found in given class", e);
        }
    }

    /**
     * Registers approved subsystem factory.
     *
     * @param <FactoryT>       Factory type
     * @param factoryClass     Class of the factory
     * @param subsystemFactory Concrete subsystem factory
     * @param priority         Subsystem priority
     */
    protected abstract <FactoryT extends SystemFactory<? extends PlayerSystem>> void registerSystem(
            @NotNull Class<FactoryT> factoryClass,
            @NotNull FactoryT subsystemFactory,
            @NotNull SystemPriority priority);

    /**
     * Gets system factory by system class.
     *
     * <p>Searches factory class inside system class and delegates it to
     * {@link #getFactory(Class)}</p>
     *
     * @param <SystemT>   System type
     * @param systemClass System class
     * @return System factory
     * @throws SystemNotFoundException If needed system not found in registry
     * @since 0.1
     */
    public <SystemT extends PlayerSystem> @NotNull SystemFactory<SystemT> getSystemFactory(
            @NotNull Class<SystemT> systemClass)
            throws SystemNotFoundException {
        try {
            Class<SystemFactory<SystemT>> factoryClass = getFactoryClass(systemClass);
            return getFactory(factoryClass);
        } catch (IllegalArgumentException e) {
            throw new SystemNotFoundException("Wrong system class.", e);
        }
    }

    /**
     * Gets inner factory class from given system class.
     *
     * @implNote
     * Each Player System must contains inner factory class.
     *
     * @param <SystemT>   System type
     * @param systemClass System class
     * @return Inner factory class from system class
     * @throws IllegalArgumentException  If needed system factory not found in registry
     * @since 0.1
     */
    protected <SystemT extends PlayerSystem> @NotNull Class<SystemFactory<SystemT>> getFactoryClass(
            Class<SystemT> systemClass) {
        Class<?>[] innerClasses = systemClass.getDeclaredClasses();
        for (Class<?> innerClass : innerClasses) {
            if (innerClass.getSuperclass() == SystemFactory.class) {
                //noinspection unchecked
                return (Class<SystemFactory<SystemT>>) innerClass;
            }
        }

        throw new IllegalArgumentException("Given class not contains any System Factory");
    }

    /**
     * Gets system factory by factory class.
     *
     * @implSpec
     * Never return {@code null}. Throw exception instead.
     *
     * @param <SystemT>    System type
     * @param factoryClass Factory class
     * @return System factory
     * @throws SystemNotFoundException If factory for needed system not found in registry
     */
    public abstract <SystemT extends PlayerSystem> @NotNull SystemFactory<SystemT> getFactory(
            @NotNull Class<? extends SystemFactory<SystemT>> factoryClass)
            throws SystemNotFoundException;

    /**
     * Unregisters all subsystems.
     *
     * @apiNote
     * Use it before plugin disabling
     */
    public abstract void unregisterAllSubsystems();

    /**
     * Unregister specified subsystem.
     *
     * @param <SubsystemT>   Subsystem type
     * @param subsystemClass Subsystem class
     */
    public <SubsystemT extends PlayerSystem> void unregisterSubsystem(@NotNull Class<SubsystemT> subsystemClass) {
        SystemFactory<? super SubsystemT> factory = this.getSubsystemFactory(subsystemClass);
        unregisterFactory(factory);
    }

    /**
     * Unregister specified factory.
     *
     * @param <SubsystemT> Subsystem factory type
     * @param factory    The factory
     */
    public abstract <SubsystemT extends PlayerSystem> void unregisterFactory(
            @NotNull SystemFactory<? extends SubsystemT> factory);
}