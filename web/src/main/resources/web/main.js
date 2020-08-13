Vue.component("message", {
  props: ["msg"],
  template: `
    <div>
        <div class="msg" v-bind:class="msg.direction">
            <div class="msg-img"></div>

            <div class="msg-bubble">
                <div class="msg-info">
                    <div class="msg-info-name">{{ msg.sender }}</div>
                </div>

                <div class="msg-text">
                    {{ msg.text }}
                </div>
            </div>
        </div>
        <hr v-if="msg.truce">
    </div>
  `
});

var vm = new Vue({
  el: "#messenger",
  data: function () {
    return {
      messages: [],
      socket: null
    };
  },
  created: function () {
    let protocol = location.protocol.startsWith("https") ? "wss" : "ws";
    this.socket = new WebSocket(protocol + "://" + location.host + "/ws");

    let vm = this;
    this.socket.onerror = function () {
      console.log("Socket error");
    };

    this.socket.onopen = function () {
      console.log("Connected to " + vm.socket.url);
    };

    this.socket.onclose = function (event) {
      let explanation = event.reason
        ? "reason: " + event.reason
        : "without a reason specified";
      console.log(
        "Disconnected with code: " + event.code + " and " + explanation
      );
    };

    this.socket.onmessage = function (event) {
      console.log("Received: " + event.data.toString());
      let data = JSON.parse(event.data.toString());
      let msg = {};
      if (data.challenger) {
        msg.sender = "Challenger";
        msg.text = data.truce ? "*yield*" : "BAM!";
        msg.direction = "left-msg";
      } else {
        msg.sender = "Opponent";
        msg.text = data.truce ? "*yield*" : "POW!";
        msg.direction = "right-msg";
      }
      msg.truce = data.truce;
      vm.messages.push(msg);
    };
  },
  methods: {
    start: function (event) {
      this.socket.send("whatever");
    }
  },
  computed: {
    shouldDisable: function () {
      let len = this.messages.length;
      return len > 0 && !this.messages[len - 1].truce;
    }
  }
});
