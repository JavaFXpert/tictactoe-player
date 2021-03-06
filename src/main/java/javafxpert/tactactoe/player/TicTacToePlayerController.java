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
package javafxpert.tactactoe.player;

import javafxpert.tactactoe.player.model.PlayerResponse;
import javafxpert.tactactoe.player.model.PredictionResponseFar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;

/**
 * @author James L. Weaver (Twitter: @JavaFXpert)
 */
@RestController
@RequestMapping("/player")
public class TicTacToePlayerController {
  public static int NUM_CELLS = 9;
  public static char X_MARK = 'X';
  public static char O_MARK = 'O';
  public static char EMPTY = 'I';

  public static double HIGHEST_VALUES_TOLERENCE = 0.1;

  private final TicTacToePlayerProperties ticTacToePlayerProperties;

  @Autowired
  public TicTacToePlayerController(TicTacToePlayerProperties ticTacToePlayerProperties) {
    this.ticTacToePlayerProperties = ticTacToePlayerProperties;
  }

  /**
   * The state of the game board
   */
  private StringBuffer gameBoard;

  /**
   * Whose turn (X or O) it is, based upon the gameBoard state
   */
  private char whoseTurn;

  /**
   * Given a game board representation, play according to the requested strategy, and return
   * the updated game board
   *
   * Example endpoint usage /player?gameBoard=XIIIOIOOX&strategy=playRandomCell
   *
   * Available strategy values are:
   *   - default
   *   - neuralNetwork
   *
   * @param gameBoardStr
   * @param strategy
   * @return the updated representation of the game board
   */
  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> calculateMove(@RequestParam(value = "gameBoard") String gameBoardStr,
                                              @RequestParam(value = "strategy", defaultValue="default") String strategy) {
    gameBoardStr = gameBoardStr.trim();

    PlayerResponse playerResponse = new PlayerResponse();
    playerResponse.setMessage("");
    playerResponse.setSuccessful(true);
    playerResponse.setGameBoardStr(gameBoardStr);

    gameBoard = new StringBuffer(gameBoardStr);

    System.out.println("gameBoard in:  " + gameBoardStr);

    if (gameBoard.length() == NUM_CELLS) {
      // TODO: Check gameBoard more thoroughly for valid state

      // Ascertain whose turn it is
      ascertainWhoseTurn();

      // TODO: Check strategy for valid value
      if (strategy.equalsIgnoreCase("default")) {
        playFirstEmptyCell();
      }
      else if (strategy.equalsIgnoreCase("neuralNetwork")) {
        consultNeuralNetwork();
      }

      playerResponse.setGameBoardStr(gameBoard.toString());

      System.out.println("gameBoard out: " + gameBoard + "\n");
    }
    else {
      String msg = "gameBoard argument has invalid state";
      playerResponse.setMessage(msg);
      playerResponse.setSuccessful(false);
      System.out.println(msg);
    }

    return Optional.ofNullable(playerResponse)
        .map(cr -> new ResponseEntity<>((Object)cr, HttpStatus.OK))
        .orElse(new ResponseEntity<>("Player request unsuccessful", HttpStatus.INTERNAL_SERVER_ERROR));

  }

  /**
   * Simple strategy that plays the first empty cell
   */
  private void playFirstEmptyCell() {
    gameBoard.setCharAt(gameBoard.indexOf(Character.toString(EMPTY)), whoseTurn);
  }

  /**
   * Strategy that consults a neural network server trained for tic tac toe
   */
  private void consultNeuralNetwork() {
    String values = "";
    for (int i = 0; i < NUM_CELLS; i++) {
      values += getOneHotStringForMark(gameBoard.charAt(i));

      // Append a comma on all but the last entries
      if (i < NUM_CELLS - 1) {
        values += ",";
      }
    }
    String predictionRequestUrl = this.ticTacToePlayerProperties.getNeuralNetworkServiceUrl(values);
    System.out.println("predictionRequestUrl: " + predictionRequestUrl);

    PredictionResponseFar predictionResponseFar = null;
    try {
      predictionResponseFar = new RestTemplate().getForObject(new URI(predictionRequestUrl),
          PredictionResponseFar.class);

      // TODO: Instead of using the prediction element returned, use the activations
      //       and randomly pick from the highest ones


      // TODO: If the prediction (or all of the activation) cells are already taken on the
      //       board, choose a random cell
      int prediction = predictionResponseFar.getPrediction();
      System.out.println("prediction is: " + prediction);

      List<Double> activations = predictionResponseFar.getActivations();
      int randomHighestOutputActivationIndex = getRandomOfHighestOutputActivations(activations);
      System.out.println("randomHighestOutputActivationIndex is: " + randomHighestOutputActivationIndex);

      if (gameBoard.charAt(randomHighestOutputActivationIndex) == EMPTY) {
        gameBoard.setCharAt(randomHighestOutputActivationIndex, whoseTurn);
      }
      else {
        playFirstEmptyCell();
      }

    }
    catch (Exception e) {
      e.printStackTrace();
      System.out.println("Caught exception when calling neural network prediction service " + e);
    }
  }

  /**
   * Given a mark (X, O, or empty), return a string represention of a one-hot vector
   * @param mark
   * @return
   */
  private String getOneHotStringForMark(char mark) {
    String oneHotString = "1,0,0"; // Default to representation for empty cell
    if (mark == X_MARK) {
      oneHotString = "0,1,0";
    }
    else if (mark == O_MARK) {
      oneHotString = "0,0,1";
    }
    return oneHotString;
  }

  /**
   * From the nine output activations in the given list of activations, find the
   * ones with virtually equally highest values (with a certain % of the highest value),
   * and return a random choice among them.
   * @param activations
   * @return the zero-based index of a tic-tac-toe cell number
   */
  private int getRandomOfHighestOutputActivations(List<Double> activations) {
    int outputActivationsStartIndex = activations.size() - NUM_CELLS;
    List<Double> outputActivations = activations.subList(outputActivationsStartIndex, activations.size());

    double highestActivation = Collections.max(outputActivations);
    List<Integer> indexOfHighestOutputActivations = new ArrayList<>();
    for (int i = 0; i < outputActivations.size(); i++) {
      if (highestActivation - outputActivations.get(i) < (highestActivation * HIGHEST_VALUES_TOLERENCE)) {
        indexOfHighestOutputActivations.add(i);
      }
    }

    int randomIndex = (int)(indexOfHighestOutputActivations.size() * Math.random());
    return indexOfHighestOutputActivations.get(randomIndex);
  }

  /**
   * Calculate whose turn it is by comparing number of X and O marks, given
   * that X always goes first
   * TODO: Decide whether to return an exception if bad state is detected
   *
   * @return X or O
   */
  private void ascertainWhoseTurn() {
    int numXs = 0;
    int numOs = 0;

    for (int idx = 0; idx < NUM_CELLS; idx++) {
      char mark = gameBoard.charAt(idx);
      if (mark == X_MARK) {
        numXs++;
      } else if (mark == O_MARK) {
        numOs++;
      }
    }

    if (numXs == numOs) {
      whoseTurn = X_MARK;
    }
    else if (numXs == numOs + 1) {
      whoseTurn = O_MARK;
    }
    else {
      System.out.println("Invalid gameBoard state: " + gameBoard);
    }
  }

}
