/*
 * Copyright 2011 GRNET S.A. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 *   1. Redistributions of source code must retain the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer.
 *
 *   2. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials
 *      provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY GRNET S.A. ``AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL GRNET S.A OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and
 * documentation are those of the authors and should not be
 * interpreted as representing official policies, either expressed
 * or implied, of GRNET S.A.
 */

package gr.grnet.aquarium.util

import java.security.MessageDigest

/**
 * Utility functions for working with Java's Crypto libraries
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object CryptoUtils {
  /**
   * The SHA-1 MessageDigest prototype. Since these implementations are generally not thread-safe,
   * we make a prototype here and clone it for every particular use-case.
   *
   * Use method `sha1Digest` from client code to get a thread-safe implementation whenever needed.
   * Also, a result taken from `sha1Digest` can be safely cached.
   *
   */
  final private[this] val SHA1Prototype = MessageDigest.getInstance("SHA-1")

  def sha1Digest = SHA1Prototype.clone().asInstanceOf[MessageDigest]

  /**
   * Get the SHA-1 digest for the provided message.
   * 
   * If the client code has a cached MessageDigest, then this is passed as the second parameter, otherwise
   * a fresh instance is used by calling `sha1Digest`.
   */
  def sha1(message: String, md: MessageDigest = sha1Digest): String = {
    if(md.getAlgorithm != SHA1Prototype.getAlgorithm) {
      throw new IllegalArgumentException("MessageDigest passed uses algorith '%s' instead of '%s'".format(md.getAlgorithm, SHA1Prototype.getAlgorithm))
    }

    md.update(message.getBytes)
    md.digest.map(i => "%02x".format(i)).mkString
  }
}