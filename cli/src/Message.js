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
    return this.contents
  }
}
