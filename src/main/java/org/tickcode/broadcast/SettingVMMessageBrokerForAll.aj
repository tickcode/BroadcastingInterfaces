/*******************************************************************************
 * Copyright (c) 2012, tickcode.org
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of tickcode, nor tickcode.org, nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.tickcode.broadcast;

public aspect SettingVMMessageBrokerForAll {
	VMMessageBroker lastInstanceCreated;
	
	/** Watch for new instances of VMMessageBroker **/
	pointcut createVMMessageBroker(VMMessageBroker _this):
		execution (VMMessageBroker+.new(..)) && this(_this) && if(VMMessageBroker.isSettingVMMessageBrokerForAll());
	
	after(VMMessageBroker _this) returning: createVMMessageBroker(_this){
		lastInstanceCreated = _this;
		AbstractMessageBroker.setUsingAspectJ(true);
	}
	
	/** Watch for new instances of Broadcast and if the static variable is set, automatically provide the
	 *  new instance of VMMessageBroker to them.
	 * @param _this
	 */
	pointcut createBroadcast(Broadcast _this):
		execution (Broadcast+.new(..)) && this(_this) && if(VMMessageBroker.isSettingVMMessageBrokerForAll());
	
	after(Broadcast _this) returning: createBroadcast(_this){
		if(lastInstanceCreated != null)
			lastInstanceCreated.add(_this);
	}
}
