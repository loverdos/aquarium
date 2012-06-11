/*
 * Copyright 2011-2012 GRNET S.A. All rights reserved.
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

package gr.grnet.aquarium.actor
package service
package rest

import gr.grnet.aquarium.ResourceLocator


/**
 * Paths recognized and served by the REST API.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
object RESTPaths {
  final val ResourcesPath = "/resources/?".r

  private def fixREDot(s: String) = s.replaceAll("""\.""", """\\.""")
  private def toResourcesPath(name: String) = "/resources/%s".format(fixREDot(name)).r
  private def toEventPath(name: String) = "/%s/([^/]+)/?".format(name).r

  final val ResourcesAquariumPropertiesPath = toResourcesPath(ResourceLocator.ResourceNames.AQUARIUM_PROPERTIES)

  final val ResourcesLogbackXMLPath = toResourcesPath(ResourceLocator.ResourceNames.LOGBACK_XML)

  final val ResourcesPolicyYAMLPath = toResourcesPath(ResourceLocator.ResourceNames.POLICY_YAML)

  final val ResourceEventPath = toEventPath("rcevent")

  final val IMEventPath = toEventPath("imevent")

  /**
   * Use this URI path to query for the user balance. The parenthesized regular expression part
   * represents the user ID.
   */
  final val UserBalancePath = "/user/([^/]+)/balance/?".r

  /**
   * Use this URI path to query for the user state.
   */
  final val UserStatePath = "/user/([^/]+)/state/?".r

  /**
   * Use this administrative URI path to ping all services used by Aquarium.
   */
  final val AdminPingAllPath = "/admin/ping/all/?".r
}