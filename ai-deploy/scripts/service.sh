#!/bin/bash
# ====================================================================
# AI-Platform Service Management
# Usage: sudo ./service.sh {start|stop|restart|status|logs}
# ====================================================================

APP_NAME="ai-platform"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}Please run as root (use sudo)${NC}"
    exit 1
fi

ACTION=${1:-status}

case "$ACTION" in
    start)
        echo -e "${GREEN}Starting $APP_NAME...${NC}"
        systemctl start $APP_NAME
        systemctl status $APP_NAME --no-pager
        ;;

    stop)
        echo -e "${YELLOW}Stopping $APP_NAME...${NC}"
        systemctl stop $APP_NAME
        ;;

    restart)
        echo -e "${YELLOW}Restarting $APP_NAME...${NC}"
        systemctl restart $APP_NAME
        sleep 5
        systemctl status $APP_NAME --no-pager
        ;;

    status)
        systemctl status $APP_NAME --no-pager
        echo ""
        echo "=== Port Check ==="
        netstat -tlnp 2>/dev/null | grep :8080 || ss -tlnp | grep :8080
        echo ""
        echo "=== Health Check ==="
        curl -s http://localhost:8080/actuator/health || echo "Service not responding"
        ;;

    logs)
        echo -e "${GREEN}Showing logs (Ctrl+C to exit)...${NC}"
        journalctl -u $APP_NAME -f -n 100
        ;;

    enable)
        systemctl enable $APP_NAME
        echo -e "${GREEN}$APP_NAME enabled to start on boot${NC}"
        ;;

    disable)
        systemctl disable $APP_NAME
        echo -e "${YELLOW}$APP_NAME disabled from starting on boot${NC}"
        ;;

    *)
        echo "Usage: $0 {start|stop|restart|status|logs|enable|disable}"
        exit 1
        ;;
esac
