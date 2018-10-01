pragma solidity ^0.4.24;

/// @title Dynamic Event Dispatch
contract EthlanceUserEvents {
    event UserEvent(address indexed _address,
		    string event_name,
		    uint event_version,
		    uint timestamp,
		    uint[] event_data);
		   

    /// @dev Emit the dynamic UserEvent.
    /// @param event_name - Name of the event.
    /// @param event_version - Version of the event.
    /// @param event_data - Array of data within the event.
    function fireEvent(string event_name,
                       uint event_version,
                       uint[] event_data)
        public {

        emit UserEvent(msg.sender,
		       event_name,
		       event_version,
		       now,
		       event_data);
    }
}
