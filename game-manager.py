#!/usr/bin/env python3
import random
import subprocess
import sys
from collections import Counter

players = dict()

def err(message):
    print(message)
    print("Usage: {} <# of runs> <bot files...>".format(sys.argv[0]))
    exit()

def main():
    args = sys.argv[1:]

    runCommands = list()
    runs = -1
    nPlayers = 1

    # Parse # of runs, compile any files and build run command list
    for bot in args:
        if bot.endswith(".java"):
            subprocess.call(["javac", bot])
            runCommands.append("java " + bot[:-5])
            players[nPlayers] = bot[:-5]
            nPlayers += 1
        elif bot.endswith(".py"):
            runCommands.append("python " + bot)
            players[nPlayers] = bot[:-2]
            nPlayers += 1
            continue
        elif bot.isdigit():
            runs = int(bot)
        else:
            err("File type not supported.")

    if runs == -1:
        err("The number of runs must be supplied.")

    sizes = [x for x in range(20, 55, 5)]

    lines = list()
    results = Counter()
    for _ in range(runs):
        size = random.choice(sizes)
        proc = subprocess.Popen(["halite.exe", "-q", "-d", "{} {}".format(size, size)] + runCommands, stdout=subprocess.PIPE)
        for line in proc.stdout:
            lines.append(line.decode("utf-8").rstrip())

        print("======================================")
        for line in lines[nPlayers:]:
            if line:
                playerNum, rank = line.split()
                playerNum = int(playerNum)
                rank = int(rank)
                print("Player #{} {} came in rank #{}".format(playerNum, players[playerNum], rank))
                results[players[playerNum]] += rank

        lines = list()

    print("############ FINAL STATS #############")
    print(results)
    print(players)

    for i in results:
        results[i] = rank / runs

    for i in range(1, nPlayers):
        player = players[i]
        print("Player #{} {} score: {}".format(i, player, (nPlayers - results[player]) / (nPlayers - 1)))



if __name__ == '__main__':
    main()
