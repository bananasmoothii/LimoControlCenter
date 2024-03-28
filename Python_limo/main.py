#!/usr/bin/env python

import math
import socket
import threading
import time

import argparse
import redis
import rospy
from geometry_msgs.msg import PoseStamped
from geometry_msgs.msg import PoseWithCovarianceStamped
from nav_msgs.msg import OccupancyGrid


ROBOT_ID = socket.gethostbyname(socket.gethostname())

# Configuration de l'analyseur d'arguments
parser = argparse.ArgumentParser(description="Exemple de script avec option --map")
parser.add_argument('--map', action='store_true', help="Activer la fonction de map")

# Analyser les arguments de la ligne de commande
args = parser.parse_args()

def keep_alive_thread():
    while True:
        # message syntax in the general channel: robot_id instruction_name [instruction_parameters]
        r.publish("general", f"{ROBOT_ID} keep_alive")
        time.sleep(0.5)


def callback_F_coord_sender_to_base(data):
    x = data.pose.pose.position.x + 2.46
    y = data.pose.pose.position.y + 2.46
    z_or = data.pose.pose.orientation.z

    if z_or >= 0:
        angle = 2 * math.acos(data.pose.pose.orientation.w)
    else:
        angle = 2 * math.pi - 2 * math.acos(data.pose.pose.orientation.w)
    print(x - 2.46, y - 2.46)
    r.publish(f"update_pos", f"{ROBOT_ID} {x},{y},{angle}")  # x, y, angle (angle in radians)



goal_coord_x = None
goal_coord_y = None


def listen_to_goal_F_goal_pose_sender_from_base():
    global goal_coord_x
    global goal_coord_y
    subscriber = r.pubsub(ignore_subscribe_messages=True)
    subscriber.subscribe("update_goal")
    while True:
        message = subscriber.get_message(timeout=None)
        if message is None:
            continue
        msg: str = message["data"]
        robot_id, goal = msg.split(' ', maxsplit=2)
        if robot_id != ROBOT_ID:
            continue
        if goal == "remove":
            print("remove goal for this robot")
        else:
            goal_coord_x, goal_coord_y = goal.split(',', maxsplit=2)


def publisher_F_goal_pose_sender_from_base():
    global goal_coord_x
    global goal_coord_y

    pub = rospy.Publisher('/move_base_simple/goal', PoseStamped, queue_size=10)
    #rate = rospy.Rate(0.5)

    msg_to_publish = PoseStamped()


    # Initial Header

    # seq = counter
    stamp = rospy.Time.now()
    frame_id = "map"

    # Goal position
    x_init_pos = float(goal_coord_x) - 2.46
    y_init_pos = float(goal_coord_y) - 2.46
    z_init_pos = 0.0

    # Goal orientation
    x_init_ori = 0.0
    y_init_ori = 0.0
    z_init_ori = 0.
    w_init_ori = 1.

    #rate.sleep()

    # Setting the hearders parameters to the publisher informations

    msg_to_publish.header.stamp = stamp
    msg_to_publish.header.frame_id = frame_id

    # Setting the goal position to the publisher informations
    msg_to_publish.pose.position.x = x_init_pos
    msg_to_publish.pose.position.y = y_init_pos
    msg_to_publish.pose.position.z = z_init_pos

    # Setting the goal orientation to the publisher informations
    msg_to_publish.pose.orientation.x = x_init_ori
    msg_to_publish.pose.orientation.y = y_init_ori
    msg_to_publish.pose.orientation.z = z_init_ori
    msg_to_publish.pose.orientation.w = w_init_ori

    rospy.loginfo(msg_to_publish)
    pub.publish(msg_to_publish)


def callback_F_map_reader(data):

    print('Sending map ...')

    liste_coord = []
    for i, p in enumerate(data.data):
        cell_x = i % 1984
        cell_y = i // 1984
        x = (cell_x - data.info.origin.position.x) * data.info.resolution - 50
        y = (cell_y - data.info.origin.position.y) * data.info.resolution - 50
        if not (-5 < x < 7 and -2 < y < 10):
            continue
        if p == 0:
            liste_coord.append(f"P{x},{y}")
        elif p > 0:
            liste_coord.append(f"W{x},{y}")
        else:
            liste_coord.append(f"U{x},{y}")
    r.publish(f"update_map", f"{ROBOT_ID} " + " ".join(liste_coord))

    print("Done sending the map to database")



if __name__ == "__main__":
    r = redis.Redis(host='172.20.10.11', port=6379, decode_responses=True)

    rospy.init_node('Le_boss')
    if args.map:
        rospy.Subscriber("/map", OccupancyGrid, callback_F_map_reader)
    rospy.Subscriber("/amcl_pose", PoseWithCovarianceStamped, callback_F_coord_sender_to_base)

    time.sleep(4)


    threading.Thread(target=keep_alive_thread, daemon=True).start()
    threading.Thread(target=listen_to_goal_F_goal_pose_sender_from_base, daemon=True).start()


    while True:
        if goal_coord_x != None:
            publisher_F_goal_pose_sender_from_base()
            goal_coord_x = None
            goal_coord_y = None
