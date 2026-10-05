# Chat to Command (C2C)

C2C watches game chat and runs configured actions when a phrase is detected.

## Profiles

Open C2C settings and choose **Global** or the current **Server** profile. Global
triggers run everywhere; server triggers run only while connected to that server.
The server profile uses the server address automatically, while the server's
display name is shown in the top-right corner of the settings screen.

## Actions

Each trigger can contain multiple actions:

- `/home` sends a command
- `Hello` sends a chat message
- `notify:Task complete` displays an action-bar notification
- `title:Ready` displays a title
- `sound:any` plays the confirmation sound

Actions run in their configured order and can have an individual delay.

## Confirmation

Enable **Confirm (Shift x5)** on a trigger to require five rapid Shift presses
before its actions are sent. C2C displays a progress bar in chat, for example
`[IIIII] C2C Confirm (5/5)`, with completed presses in green and remaining
presses in gray.
