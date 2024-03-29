tmux new-session -d -s limo_base "roslaunch limo_base limo_base.launch"
sleep 5
tmux new-session -d -s limo_bringup_start "roslaunch limo_bringup limo_start.launch"
sleep 5
tmux new-session -d -s limo_bringup_nav "roslaunch limo_bringup limo_navigation_ackerman.launch"

echo "Limo launched"