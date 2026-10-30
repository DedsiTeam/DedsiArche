import { createBrowserRouter } from 'react-router-dom'
import { AppLayout } from '../layouts/AppLayout'
import { HomePage } from '../pages/HomePage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { LoginPage } from '../pages/LoginPage'
import { UsersPage } from '../pages/UsersPage'
import { ManagedSystemsPage } from '../pages/ManagedSystemsPage'
import { PositionsPage } from '../pages/PositionsPage'
import { PermissionsPage } from '../pages/PermissionsPage'
import { RequireAuth } from './RequireAuth'

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
        path: '/',
        element: <AppLayout />,
        children: [
          { index: true, element: <HomePage /> },
          { path: 'users', element: <UsersPage /> },
          { path: 'systems', element: <ManagedSystemsPage /> },
          { path: 'permissions', element: <PermissionsPage /> },
          { path: 'positions', element: <PositionsPage /> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
])
