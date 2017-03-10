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
    const chalk = require('chalk')
    if (this.command === 'echo') {
      return (chalk.cyan.bold('echo:\n' + this.timestamp + ' ' + this.username + ' (echo) ' + this.contents))
    } else if (this.command === 'broadcast') {
      return chalk.red.bold('broadcast:\n' + this.timestamp + ' ' + this.username + ' (all) ' + this.contents)
    } else if (this.command === '@') {
      return chalk.magenta('direct message:\n' + this.timestamp + ' ' + this.username + ' (whipser) ' + this.contents)
    } else if (this.command === 'connect') {
      return chalk.green.bold('connection alert:\n' + this.timestamp + ': ' + this.username + ' has connected')
    } else if (this.command === 'disconnect') {
      return chalk.gray('connection alert:\n' + this.timestamp + ': ' + this.username + ' has disconnected')
    } else if (this.command === 'users') {
      return chalk.yellow.dim('users:\n' + this.timestamp + ': currently connected users:\n' + this.contents)
    } else if (this.command === '!@') {
      return this.contents
    } else if (this.command === 'fail') {
      return this.contents
    }
  }
}
