import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { apiService } from '@/services/api';
import type { User } from '@/types';

interface AuthContextType {
  user: User | null;
  token: string | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  isAuthenticated: boolean;
  hasCommerce: boolean;
  checkCommerce: () => Promise<boolean>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [hasCommerce, setHasCommerce] = useState(false);

  /**
   * Verifica si el usuario tiene un commerce asociado
   */
  const checkCommerce = useCallback(async (): Promise<boolean> => {
    try {
      const commerce = await apiService.checkCommerce();
      const hasCommerceValue = !!commerce;
      setHasCommerce(hasCommerceValue);
      
      // Actualizar el usuario con el commerce_id si existe
      if (commerce) {
        setUser((prevUser) => {
          if (prevUser) {
            return { ...prevUser, commerce_id: commerce.id };
          }
          return prevUser;
        });
      }
      
      return hasCommerceValue;
    } catch (error) {
      setHasCommerce(false);
      return false;
    }
  }, []);

  useEffect(() => {
    // Check for stored token and user
    const storedToken = localStorage.getItem('auth_token');
    const storedUser = localStorage.getItem('user');

    if (storedToken && storedUser) {
      setToken(storedToken);
      const parsedUser = JSON.parse(storedUser);
      setUser(parsedUser);
      // Verify token is still valid
      apiService.getCurrentUser()
        .then(async (currentUser) => {
          setUser(currentUser);
          // Verificar commerce después de obtener el usuario actualizado
          await checkCommerce();
        })
        .catch(() => {
          // Token invalid, clear storage
          localStorage.removeItem('auth_token');
          localStorage.removeItem('user');
          setToken(null);
          setUser(null);
          setHasCommerce(false);
        })
        .finally(() => {
          setLoading(false);
        });
    } else {
      setLoading(false);
    }
  }, [checkCommerce]);

  const login = async (email: string, password: string) => {
    const response = await apiService.login(email, password);
    setToken(response.token);
    setUser(response.user);
    localStorage.setItem('auth_token', response.token);
    localStorage.setItem('user', JSON.stringify(response.user));
    
    // Verificar commerce después de login
    await checkCommerce();
  };

  const register = async (name: string, email: string, password: string) => {
    const response = await apiService.register(name, email, password);
    setToken(response.token);
    setUser(response.user);
    localStorage.setItem('auth_token', response.token);
    localStorage.setItem('user', JSON.stringify(response.user));
    
    // Verificar commerce después de registro
    await checkCommerce();
  };

  const logout = async () => {
    try {
      await apiService.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      setToken(null);
      setUser(null);
      setHasCommerce(false);
      localStorage.removeItem('auth_token');
      localStorage.removeItem('user');
    }
  };

  const value: AuthContextType = {
    user,
    token,
    loading,
    login,
    register,
    logout,
    isAuthenticated: !!token && !!user,
    hasCommerce,
    checkCommerce,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

