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

package gr.grnet.aquarium.store.mongodb
package confmodel

import com.mongodb.ServerAddress

////////////////////////////////////////////////////////////////////////////
// The WriteConcerns are as follows:
////////////////////////////////////////////////////////////////////////////
// NONE: No exceptions are raised, even for network issues
// NORMAL: Exceptions are raised for network issues, but not server errors
// SAFE: Exceptions are raised for network issues, and server errors; waits on a server for the write operation
// MAJORITY: Exceptions are raised for network issues, and server errors; waits on a majority of servers for the write operation
// FSYNC_SAFE: Exceptions are raised for network issues, and server errors; the write operation waits for the server to flush the data to disk
// JOURNAL_SAFE: Exceptions are raised for network issues, and server errors; the write operation waits for the server to group commit to the journal file on disk
// REPLICAS_SAFE: Exceptions are raised for network issues, and server errors; waits for at least 2 servers for the write operation
////////////////////////////////////////////////////////////////////////////

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case class MongoDBConfigurationModel(
    hosts: List[ServerAddressConfigurationModel],
    slaveOK: Boolean,
    writeConcern: String)