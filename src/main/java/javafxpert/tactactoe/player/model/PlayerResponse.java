/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javafxpert.tactactoe.player.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;

/**
 * @author James L. Weaver (Twitter: @JavaFXpert)
 */
@JsonRootName("playerTurnResult")
@JsonPropertyOrder({"gameBoard"})
public class PlayerResponse {
  @JsonProperty("gameBoard")
  private String gameBoardStr;

  @JsonProperty("successful")
  private boolean successful;

  @JsonProperty("message")
  private String message;

  public PlayerResponse() {
  }

  public PlayerResponse(String gameBoardStr, boolean successful, String message) {
    this.gameBoardStr = gameBoardStr;
    this.successful = successful;
    this.message = message;
  }

  public String getGameBoardStr() {
    return gameBoardStr;
  }

  public void setGameBoardStr(String gameBoardStr) {
    this.gameBoardStr = gameBoardStr;
  }

  public boolean isSuccessful() {
    return successful;
  }

  public void setSuccessful(boolean successful) {
    this.successful = successful;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return "PlayerResponse{" +
        "gameBoardStr='" + gameBoardStr + '\'' +
        ", successful=" + successful +
        ", message='" + message + '\'' +
        '}';
  }
}
