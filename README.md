# League multichat


League of legends multi chat emulates moderated chat rooms via the client's build in chat service. 



<a href="https://www.youtube.com/watch?v=w1umh_Wg9uk"><img src="https://user-images.githubusercontent.com/9025925/52348614-0ce4f980-2a25-11e9-91db-8cd0c8c16696.jpg"/></a>


__Note:__ I am a strong advocate of open source projects but due to the nature of potential spammers abusing the internal league
api, only a small fraction of the code will be available in this repository. 

## Usage

- Send a friend request to the user __MultiChat__. Another account __MultiChat[x-y]__ will send you a request you have to accept.
- Open the chat window and you are ready to go. Type <code>!rooms</code> to display public rooms and <code>!join</code> to join a room
- After going offline you will need to send a new friend request to the bot. This is done to ensure that we are not running out of precious friend list space too quickly. 

### Commands

<i>Italic parameters</i> are optional

<table>
  <tr> <th>Command</th> <th>Description</th> <th>Example</th></tr>
  
    
  
  ## Chat room specific
  <tr>  <td>!help</td> <td>Display a list of all available commands.</td> <td><code>!help</code></td></tr>
  <tr>  <td>!rooms</td> <td>List all public chatrooms without password</td> <td><code>!list</code></td></tr>
  <tr>  <td>!mute</td> <td>List all players you have muted</td> <td><code>!mute</code></td></tr>
  <tr>  <td>!mute playername</td> <td>Mute a player. You will no longer see messages send by this person</td> <td><code>!mute Patex</code></td></tr>
  <tr>  <td>!unmute playername</td> <td>Unmute a player. Start receiving messages send by this player again.</td> <td><code>!unmute Patex</code></td></tr>
  <tr>  <td>!join roomname <i>password</i></td> <td>Join a chatroom. If the room does not exist create and join it.</br> The roomname is case insensitive. If you are already part of a chatroom leave it.</td> <td><code>!join Lfg</code></td></tr>
  <tr>  <td>!mode</td> <td>Display the currently used chatmode</td> <td><code>!mode</code></td></tr>
  <tr>  <td>!mode newMode</td> <td>Set when the bot is allowed to send messages. Default: Away*</td> <td><code>!mode away</code></td></tr>
 
  
  <tr align=center> <td colspan = 3 ><b>Chatoom specific commands. May only be used when join a room.</b></td>  </tr>
  <tr>  <td>!leave</td> <td>Leave the currently joined chatroom</td> <td><code>!leave</code></td></tr>
  <tr>  <td>!online</td> <td>Display all online players in the chatroom</td> <td><code>!online</code></td></tr>
   <tr>  <td>!ban</td> <td>List all banned players</td> <td><code>!ban</td></tr>
  <tr>  <td>!ban playername</td> <td>Ban the player. The player will be kicked from the room and won't be able to join it again. Requires chat room admin permission.</td> <td><code>!ban Patex</code></td></tr>
  <tr>  <td>!unban playername</td> <td>Unban the player. Requires chat room admin permission.</td> <td><code>!admin Patex</code></td></tr>
  <tr>  <td>!admin playername</td> <td>Promote the player to be an admin of this room. Requires chat room admin permission.</td> <td><code>!unban Patex</code></td></tr>
   <tr align=center> <td colspan = 3 ><b>Looking for group specific commands. May only be used when join a room.</b></td>  </tr>
   <tr>  <td>!aram</td> <td>Start searching for an same random all mid game. (Every team gets assigned the same champions)</td> <td><code>!aram</code></td></tr>
</table>

### Chatmodes

Chatmodes are increasingly permissive. If mobile is chosen it will also include all permissions set in dnd, away and chat.

<table>
  <tr><th>Modes</th><th>Description</th></tr>
  <tr><td><code>chat</code></td><td>Only send messages when available (green dot in friend list)</td></tr>
  <tr><td><code>away</code></td><td>Also receive messages when away (red dot in friend list)</td></tr>
  <tr><td><code>dnd</code></td><td>Additionally receive messages in champ select and in game</td></tr>
  <tr><td><code>mobile</code></td><td>Also receive messages </td></tr>
  <tr><td><code>offline</code></td><td>Currently not supported</td></tr>
  <tr><td><code>unknown</code></td><td>Currently not supported</td></tr>
 </table>

## How it works

Historically developers were able to interact with the riot chat service simply by using the XMPP protocol. Sadly this opportunity was taken away with RSO and different means of access had to be found. After a few weekends we have access to about every functionality the client offers including hosting games. 
The program hooks into running league clients and is able to query and call the internal league api endpoints.To circumvent the issue of limited friend lists multiple accounts are chained together each approximately supporting 300 users. Clogged chat rooms were an issue back in the days. To not let this happen with this bot users going offline will be automatically kicked from the friend list.

<span align=center>
<img src="https://user-images.githubusercontent.com/9025925/51691188-9c88b200-1ffa-11e9-964f-eceba779104a.png"/>
</span>



## FAQ/HELP

### The bot does not accept my friend requests

The bot may be offline or does not have space on the friend list. Sadly we can't notify the user and immediately kick him again due to the fact that people not present on your friend list anymore aren't able to read messages sent to them.

### How do you want to handle people not picking the champs assigned to them in the aram games?

Well I am not Riot. I don't monetarily gain from this service and am strict against flaming and any other kind of abuse. If someone does not comply with the rules nothing is in our way to outright ban people from using this. Measures will be put in place if this bot ever goes online.

....

### Feature requests
Do you have any idea what the bot should be able to do? Any feature requests?  Elo gated chat rooms?
automatic group search queries e.g. `lfg sr+ 3 [elo]`. We have the possibility to access any webpage and can host games and such. Feel free to come up with any idea you like.



