/*
 * This file is part of Mixin, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.asm.launch;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.mixin.injection.invoke.arg.ArgsClassGenerator;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Constants;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import cpw.mods.jarhandling.JarMetadata;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.SecureJar.Provider;
import cpw.mods.jarhandling.VirtualJar;
import cpw.mods.modlauncher.api.IModuleLayerManager;

/**
 * Service for handling transforms mixin under ModLauncher, most of the
 * functionality is provided by the abstract base class used for pre-9 versions
 * of ModLauncher, though we also handle SecureJarHandler requirements here, for
 * modlauncher 10+ 
 */
public class MixinTransformationService extends MixinTransformationServiceAbstract {

    private static final String VIRTUAL_JAR_CLASS = "cpw.mods.jarhandling.VirtualJar";

    @Override
    public List<Resource> completeScan(final IModuleLayerManager layerManager) {
        try {
            Path codeSource = Path.of(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());

            if (this.detectVirtualJar(layerManager)) {
                try {
                    return ImmutableList.<Resource>of(this.createVirtualJar(codeSource));
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            
            try {
                return ImmutableList.<Resource>of(this.createShim(codeSource));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        } catch (Throwable th) {
            th.printStackTrace();
            return super.completeScan(layerManager);
        }
    }

    private boolean detectVirtualJar(final IModuleLayerManager layerManager) {
        try {
            MixinService.getService().getClassProvider().findClass(MixinTransformationService.VIRTUAL_JAR_CLASS, false);
            return true;
        } catch (ClassNotFoundException ex) {
            // VirtualJar not supported
            return false;
        }
    }

    private Resource createVirtualJar(final Path codeSource) throws URISyntaxException {
        VirtualJar jar = new VirtualJar("mixin_synthetic", codeSource, Constants.SYNTHETIC_PACKAGE, ArgsClassGenerator.SYNTHETIC_PACKAGE);
        return new Resource(IModuleLayerManager.Layer.GAME, ImmutableList.<SecureJar>of(jar));
    }

    @SuppressWarnings("removal")
    private Resource createShim(final Path codeSource) throws URISyntaxException {
        final Path path = codeSource.resolve("mixin_synthetic");
        final Set<String> packages = ImmutableSet.<String>of(Constants.SYNTHETIC_PACKAGE, ArgsClassGenerator.SYNTHETIC_PACKAGE);
        SecureJar jar = SecureJar.from(sj -> JarMetadata.fromFileName(path, packages, ImmutableList.<Provider>of()), codeSource);
        return new Resource(IModuleLayerManager.Layer.GAME, ImmutableList.<SecureJar>of(jar));
    }
    
}
