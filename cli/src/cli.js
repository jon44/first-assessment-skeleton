import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let host = 'localhost'
let port = 8080
let lastCommand = 'empty'
let includeCommand = false

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> [hostname] [portnum]')
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    if (args.hostname) { host = args.hostname }
    if (args.portnum) { port = args.portnum }
    server = connect({ host: host, port: port }, () => {
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
      this.log(Message.fromJSON(buffer).toString())
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {
    const [ newCommand, ...rest ] = words(input, /[^, ]+/g)
    const command = getCommand(newCommand)
    const contents = getContents(rest, newCommand).join(' ')

    if (command === 'disconnect') {
      server.end(new Message({ username, command }).toJSON() + '\n')
    } else if (command === 'echo' || command === 'broadcast' || command[0] === '@' || command === 'users') {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else {
      this.log(`Command <${command}> was not recognized`)
    }

    callback()
  })

function getCommand (command) {
  if (command === 'connect' ||
     command === 'disconnect' ||
     command === 'echo' ||
     command === 'broadcast' ||
     command[0] === '@' ||
     command === 'users') {
    lastCommand = command
    includeCommand = false
    return command
  } else if (lastCommand === 'empty') {
    return command
  } else {
    includeCommand = true
    return lastCommand
  }
}

function getContents (rest, command) {
  if (includeCommand) {
    return [command, ...rest]
  } else {
    return rest
  }
}
