/**
 * Copyright (c) 2012-2018, Andy Janata
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of conditions
 *   and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, this list of
 *   conditions and the following disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.socialgamer.cah.handlers;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.google.inject.Inject;

import net.socialgamer.cah.Constants.AjaxOperation;
import net.socialgamer.cah.Constants.AjaxRequest;
import net.socialgamer.cah.Constants.ErrorCode;
import net.socialgamer.cah.Constants.LongPollEvent;
import net.socialgamer.cah.Constants.LongPollResponse;
import net.socialgamer.cah.Constants.ReturnableData;
import net.socialgamer.cah.RequestWrapper;
import net.socialgamer.cah.data.Game;
import net.socialgamer.cah.data.GameManager;
import net.socialgamer.cah.data.QueuedMessage.MessageType;
import net.socialgamer.cah.data.User;
import net.socialgamer.cah.util.ChatFilter;


/**
 * Handler for chat messages.
 *
 * @author Andy Janata (ajanata@socialgamer.net)
 */
public class GameChatHandler extends GameWithPlayerHandler {

  private static final Logger LOG = Logger.getLogger(GameChatHandler.class);
  public static final String OP = AjaxOperation.GAME_CHAT.toString();

  private final ChatFilter chatFilter;

  @Inject
  public GameChatHandler(final GameManager gameManager, final ChatFilter chatFilter) {
    super(gameManager);
    this.chatFilter = chatFilter;
  }

  @Override
  public Map<ReturnableData, Object> handleWithUserInGame(final RequestWrapper request,
      final HttpSession session, final User user, final Game game) {
    final Map<ReturnableData, Object> data = new HashMap<ReturnableData, Object>();
    final boolean emote = request.getParameter(AjaxRequest.EMOTE) != null
        && Boolean.valueOf(request.getParameter(AjaxRequest.EMOTE));

    if (request.getParameter(AjaxRequest.MESSAGE) == null) {
      return error(ErrorCode.NO_MSG_SPECIFIED);
    } else {
      final String message = request.getParameter(AjaxRequest.MESSAGE).trim();

      final ChatFilter.Result filterResult = chatFilter.filterGame(user, message);
      switch (filterResult) {
        case OK:
          // nothing to do
          break;
        case TOO_FAST:
          return error(ErrorCode.TOO_FAST);
        case TOO_LONG:
          return error(ErrorCode.MESSAGE_TOO_LONG);
        case NO_MESSAGE:
          return error(ErrorCode.NO_MSG_SPECIFIED);
        default:
          LOG.error(String.format("Unknown chat filter result %s", filterResult));
      }

      final HashMap<ReturnableData, Object> broadcastData = new HashMap<ReturnableData, Object>();
      broadcastData.put(LongPollResponse.EVENT, LongPollEvent.CHAT.toString());
      broadcastData.put(LongPollResponse.FROM, user.getNickname());
      broadcastData.put(LongPollResponse.MESSAGE, message);
      broadcastData.put(LongPollResponse.FROM_ADMIN, user.isAdmin());
      broadcastData.put(LongPollResponse.ID_CODE, user.getIdCode());
      broadcastData.put(LongPollResponse.SIGIL, user.getSigil().toString());
      broadcastData.put(LongPollResponse.GAME_ID, game.getId());
      broadcastData.put(LongPollResponse.EMOTE, emote);
      game.broadcastToPlayers(MessageType.CHAT, broadcastData);
    }

    return data;
  }
}
