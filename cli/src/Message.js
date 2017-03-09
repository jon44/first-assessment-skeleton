export class Message {
  static fromJSON (buffer) {
    return new Message(JSON.parse(buffer.toString()))
  }

  constructor ({ timestamp, username, command, contents }) {
    this.timestamp = timestamp
    this.username = username
    this.command = command
    this.contents = contents
  }

  toJSON () {
    return JSON.stringify({
      timestamp: this.timestamp,
      username: this.username,
      command: this.command,
      contents: this.contents
    })
  }

  toString () {
    if (this.command === 'echo') {
      return 'echo:\n' + this.timestamp + ' ' + this.username + ' (echo) ' + this.contents
    } else if (this.command === 'broadcast') {
      return 'broadcast:\n' + this.timestamp + ' ' + this.username + ' (all) ' + this.contents
    } else if (this.command === '@') {
      return 'direct message:\n' + this.timestamp + ' ' + this.username + ' (whipser) ' + this.contents
    } else if (this.command === 'connect') {
      return 'connection alert:\n' + this.timestamp + ' ' + this.username + ' has connected'
    } else if (this.command === 'disconnect') {
      return 'connection alert:\n' + this.timestamp + ' ' + this.username + ' has disconnected'
    } else if (this.command === 'users') {

    }
  }
}
