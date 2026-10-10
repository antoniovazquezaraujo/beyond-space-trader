/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.audio;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/**
 * The loader must work inside the jar, the only place the game really runs from.
 * There the resource stream ({@code JarURLInputStream}) does not support
 * mark/reset and {@code AudioSystem} needs it, so {@code SampleLibrary.read} has
 * to wrap it; on the directory classpath of the tests the stream is already
 * buffered.
 *
 * <p>This test builds a jar with the real class file and a WAV fixture, loads it
 * in an isolated classloader and asks for a key, exactly as the packaged game
 * does. Without the buffered wrap it fails with "mark/reset not supported".
 */
class SampleLibraryJarTest {
  @Test
  void theLibraryLoadsAWavFromInsideAJar(@TempDir Path dir) throws Exception {
    Path jar = dir.resolve("sounds.jar");
    writeJar(jar);

    try(URLClassLoader loader = new URLClassLoader(new URL[] {jar.toUri().toURL()},
        ClassLoader.getPlatformClassLoader())) {
      Class<?> libraryClass = Class.forName("org.gts.bst.audio.SampleLibrary", true, loader);
      Constructor<?> constructor = libraryClass.getDeclaredConstructor();
      constructor.setAccessible(true);
      Object library = constructor.newInstance();
      Method sample = libraryClass.getDeclaredMethod("sample", String.class);
      sample.setAccessible(true);

      float[] samples = (float[]) sample.invoke(library, "ships/gnat");

      assertNotNull(samples, "the WAV inside the jar must load (mark/reset wrapped)");
      assertTrue(samples.length > 0, "and its samples must be read");
    }
  }

  private static void writeJar(Path jar) throws IOException {
    try(JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
      add(out, "org/gts/bst/audio/SampleLibrary.class", resource("org/gts/bst/audio/SampleLibrary.class"));
      add(out, "org/gts/bst/audio/SampleLibrary$ResourceOpener.class",
          resource("org/gts/bst/audio/SampleLibrary$ResourceOpener.class"));
      add(out, "sounds/ships/gnat-1.wav", resource("sounds/ships/gnat-1.wav"));
    }
  }

  private static void add(JarOutputStream out, String name, byte[] bytes) throws IOException {
    out.putNextEntry(new JarEntry(name));
    out.write(bytes);
    out.closeEntry();
  }

  private static byte[] resource(String name) throws IOException {
    try(InputStream in = SampleLibraryJarTest.class.getClassLoader().getResourceAsStream(name)) {
      assertNotNull(in, name + " is on the test classpath");
      return in.readAllBytes();
    }
  }
}
