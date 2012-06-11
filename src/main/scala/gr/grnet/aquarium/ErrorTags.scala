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

package gr.grnet.aquarium

/**
 * Describe all possible error tags.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

final object ErrorTags {
  final case class tag private[ErrorTags](app: String, group: String, number: String) {
    @inline
    def message(fmt: String, args: Any*) = {
      "[%s_%s_%s]".format(app, group, number) + fmt.format(args:_*)
    }
  }

  private[ErrorTags] final object Apps {
    private[ErrorTags] final val AQU = "AQU"
  }

  private[ErrorTags] final object Groups {
    final val BAL = "BAL"
  }

  import Apps.AQU
  import Groups.BAL

  private[ErrorTags] implicit def tuple3ToTag(agn: (String, String, String)) = tag(agn._1, agn._2, agn._3)

  // Errors related to REST API: GetUserBalance
  final val AQU_BAL_001 = (AQU, BAL, "001")
  final val AQU_BAL_002 = (AQU, BAL, "002")
  final val AQU_BAL_003 = (AQU, BAL, "003")
  final val AQU_BAL_004 = (AQU, BAL, "004")
  final val AQU_BAL_005 = (AQU, BAL, "005")
  final val AQU_BAL_006 = (AQU, BAL, "006")
}
