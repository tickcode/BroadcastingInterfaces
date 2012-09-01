package org.tickcode.broadcast;

public aspect SettingVMMessageBrokerForAll {
	VMMessageBroker lastInstanceCreated;
	
	/** Watch for new instances of VMMessageBroker **/
	pointcut createVMMessageBroker(VMMessageBroker _this):
		execution (VMMessageBroker+.new(..)) && this(_this) && if(VMMessageBroker.isSettingVMMessageBrokerForAll());
	
	after(VMMessageBroker _this) returning: createVMMessageBroker(_this){
		lastInstanceCreated = _this;
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
