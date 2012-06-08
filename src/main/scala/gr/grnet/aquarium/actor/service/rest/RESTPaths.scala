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


/**
 * Paths recognized and served by the REST API.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
object RESTPaths {
  final val ResourcesLogbackXML = "/resources/logback\\.xml".r

  final val ResourcesAquariumProperties = "/resources/aquarium\\.properties".r

  final val ResourcesPolicyYAML = "/resources/policy\\.yaml".r

  final val RolesAgreementsMap = "/resources/roles-agreements\\.map".r

  final val ResourceEventPath = "/rcevent/([^/]+)/?".r

  final val IMEventPath = "/imevent/([^/]+)/?".r

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
  final val AdminPingAll = "/admin/ping/all/?".r
}