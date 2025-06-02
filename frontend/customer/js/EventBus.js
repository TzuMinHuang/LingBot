export class EventBus {
    constructor() {
      this.events = {};
    }
  
    on(event, handler) {
      if (!this.events[event]) this.events[event] = [];
      this.events[event].push(handler);
    }
  
    emit(event, data) {
      (this.events[event] || []).forEach(fn => fn(data));
    }
  
    off(event, handler) {
      this.events[event] = (this.events[event] || []).filter(fn => fn !== handler);
    }
  }
  